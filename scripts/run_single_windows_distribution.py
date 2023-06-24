import os
import shutil

# Configuration parameters
node_sizes = {
    128: 1234567,
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

find_modes = [0, 1 , 2, 3]

config_files = ['..\\simulator\\config\\kademlia.cfg'] # List of config file paths
output_dir = '..\\simulator\\output' # Output directory path
log_dir = '..\\simulator\\logs' # Log directory path
base_path = '..\\simulator'

jar_paths = [
    os.path.join(base_path, 'lib', 'djep-1.0.0.jar'),
    os.path.join(base_path, 'lib', 'jep-2.3.0.jar'),
    os.path.join(base_path, 'lib', 'gs-core-2.0.jar'),
    os.path.join(base_path, 'lib', 'mbox2-1.0.jar'),
    os.path.join(base_path, 'lib', 'gs-ui-swing-2.0.jar'),
]

target_path = os.path.join(base_path, 'target', 'service-discovery-1.0-SNAPSHOT.jar')

classpath = ';'.join(jar_paths)

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

        # Generate a unique name for the copied config file 
        unique_name = f"{size}_{find_mode}_{traffic_step}"
        config_copy = os.path.join(output_dir_config, f"{os.path.splitext(config_basename)[0]}_{unique_name}.cfg")

        # Create a separate copy of the config file for this simulation
        shutil.copy(config_file, config_copy)

        # Modify the parameters in the config file based on the size, seed, and find mode
        change_key(config_copy, "SIZE", str(size))
        change_key(config_copy, "random.seed", str(seed))
        change_key(config_copy, "FINDMODE", str(find_mode))
        change_key(config_copy, "TRAFFIC_STEP", traffic_step)
        change_key(config_copy, "OBSERVER_STEP", observer_step)

        # Run the simulation
        command = f'java -Xmx200000m -cp "{classpath};{target_path}" -ea peersim.Simulator "{config_copy}" > nul 2> nul'

        os.system(command)

        # Move the generated log files to the appropriate log folder/directory
        log_dir_config = os.path.join(log_dir, f"log_{size}_{find_mode}")
        os.makedirs(log_dir_config, exist_ok=True)

        shutil.move(os.path.join(log_dir, 'count.csv'), os.path.join(log_dir_config, f"count_{size}_{find_mode}_{traffic_step}.csv"))
        shutil.move(os.path.join(log_dir, 'messages.csv'), os.path.join(log_dir_config, f"messages_{size}_{find_mode}_{traffic_step}.csv"))
        shutil.move(os.path.join(log_dir, 'operation.csv'), os.path.join(log_dir_config, f"operation_{size}_{find_mode}_{traffic_step}.csv"))
        shutil.move(os.path.join(log_dir, 'routingtable.csv'), os.path.join(log_dir_config, f"routing_table_{size}_{find_mode}_{traffic_step}.csv"))

        print("Simulation completed:", config_file, "with size", size, "seed", seed, "find mode", find_mode)

    except Exception as e:
        print("Error occurred during simulation:", e)

def main() -> int:
    shutil.rmtree(output_dir, ignore_errors=True)
    shutil.rmtree(log_dir, ignore_errors=True)

    os.makedirs(output_dir, exist_ok=True)
    os.makedirs(log_dir, exist_ok=True)
    
    traffic_steps = [14400, 7200, 3600, 1200, 600, 300]
    observer_steps = [14399, 7199, 3599, 1199, 599, 299]

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


    return 0

if __name__ == '__main__':
    main()
