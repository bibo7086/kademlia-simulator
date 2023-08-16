import os
import shutil
import csv
from multiprocessing import Pool, cpu_count

# Configuration parameters
node_sizes = {
    128: 123456789,
    # 256: 67890,
    # 512: 45678,
    # 1024: 98765,
    # 2048: 54321,
    # 4096: 24680,
    # 8192: 13579,
    # 16384: 55555,
    # 32768: 88888,
    # 65536: 22222, 
    # 10000: 319132, 
}

find_modes = [0, 3]  # List of find modes

config_files = ["../simulator/config/kademlia.cfg"] # List of config file paths
output_dir = "../simulator/output/"  # Output directory path
log_dir = "../simulator/logs/"  # Log directory path

def change_key(file, key, val):
    with open(file, 'r') as f:
        lines = f.readlines()

    with open(file, 'w') as f:
        for line in lines:
            if key in line and line.split()[0] == key:
                line = f"{key} {val}\n"
            f.write(line)

def run_sim(config_file, size, seed, find_mode, traffic_step, observer_step):
    try:
        # Get the base name of the config file 
        config_basename = os.path.basename(config_file)

        # Create a separate directory for each configuration
        config_name = os.path.splitext(os.path.basename(config_file))[0]
        output_dir_config = os.path.join(output_dir, config_name)
        os.makedirs(output_dir_config, exist_ok=True)

        # Generate a unique name for the to be copied config file 
        unique_name = f"{size}_{find_mode}"
        # config_copy = os.path.join(os.path.dirname(output_dir_config), f"{os.path.splitext(config_basename)[0]}_{unique_name}.cfg")
        config_copy = os.path.join(output_dir_config, f"{os.path.splitext(config_basename)[0]}_{unique_name}.cfg")

        # Create a separate copy of the config file for this simulation
        shutil.copy(config_file, config_copy)

        # Modify the parameters in the config file based on the size, seed, find mode, traffic step, and observer step
        change_key(config_copy, "SIZE", size)
        change_key(config_copy, "random.seed", seed)
        change_key(config_copy, "FINDMODE", find_mode)
        change_key(config_copy, "TRAFFIC_STEP", traffic_step)
        change_key(config_copy, "OBSERVER_STEP", observer_step)

        # Run the simulation
        os.system(f"java -Xmx200000m -cp ../simulator/lib/djep-1.0.0.jar:../simulator/lib/jep-2.3.0.jar:../simulator/target/service-discovery-1.0-SNAPSHOT.jar:../simulator/lib/gs-core-2.0.jar:../simulator/lib/pherd-1.0.jar:../simulator/lib/mbox2-1.0.jar:../simulator/lib/gs-ui-swing-2.0.jar -ea peersim.Simulator {config_copy} > /dev/null 2> /dev/null")

        # Move the generated log files to the appropriate log folder/directory
        log_dir_config = os.path.join(log_dir, f"log_{size}_{find_mode}")
        os.makedirs(log_dir_config, exist_ok=True)

        shutil.move(os.path.join(log_dir, f'count_{size}_{traffic_step}_{find_mode}.csv'), os.path.join(log_dir_config, f"count_{size}_{find_mode}_{traffic_step}.csv"))
        # shutil.move(os.path.join(log_dir, f'messages_{traffic_step}_{find_mode}.csv'), os.path.join(log_dir_config, f"messages_{size}_{find_mode}_{traffic_step}.csv"))
        shutil.move(os.path.join(log_dir, f'operation_{size}_{traffic_step}_{find_mode}.csv'), os.path.join(log_dir_config, f"operation_{size}_{find_mode}_{traffic_step}.csv"))
        # shutil.move(os.path.join(log_dir, 'routingtable_{observer_step}.csv'), os.path.join(log_dir_config, f"routing_table_{size}_{find_mode}_{traffic_step}.csv"))

        print("Simulation completed:", config_file, "with size", size, "seed", seed, "find mode", find_mode)

    except Exception as e: 
        print("Error occurred during simulation:", e)

def main() -> int: 
    os.system(f"rm -rf {output_dir}")
    os.system(f"rm -rf {log_dir}")

    os.makedirs(output_dir, exist_ok=True)
    os.makedirs(log_dir, exist_ok=True)

    traffic_steps = [100000]
    observer_steps = [100000]

    for config_file in config_files:
        for find_mode in find_modes:
            for size, seed in node_sizes.items():
                print("Running", config_file, "with size", size, "seed", seed, "find mode", find_mode)
                for i in range(len(traffic_steps)):
                    # Update the traffic step and observer step
                    traffic_step = traffic_steps[i]
                    observer_step = observer_steps[i]
                    
                    # Run the simulation
                    run_sim(config_file, size, seed, find_mode, traffic_step, observer_step)

if __name__ == '__main__':
    main()
