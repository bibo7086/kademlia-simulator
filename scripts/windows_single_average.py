import os
import shutil
from common_code import *

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
    # 5000: 654654, 
    # 10000: 319132, 

}

find_modes = [0, 3]

config_files = windows_config_files # List of config file paths
output_dir = windows_output_dir # Output directory path
log_dir =  windows_log_dir # Log directory path
base_path = windows_base_path

jar_paths = windows_jar_paths
target_path = windows_target_path

classpath = windows_classpath

# def run_sim(config_file, size, seed, find_mode, traffic_step, observer_step):
def run_sim(config_file, size, seed, find_mode):
    try:
        # Get the base name of the config file 
        config_basename = os.path.basename(config_file)

        # Create a separate directory for each configuration
        config_name = os.path.splitext(os.path.basename(config_file))[0]
        output_dir_config = os.path.join(output_dir, config_name)
        os.makedirs(output_dir_config, exist_ok=True)

        # Generate a unique name for the copied config file 
        unique_name = f"{size}_{find_mode}"
        config_copy = os.path.join(output_dir_config, f"{os.path.splitext(config_basename)[0]}_{unique_name}.cfg")

        # Create a separate copy of the config file for this simulation
        shutil.copy(config_file, config_copy)

        # Modify the parameters in the config file based on the size, seed, and find mode
        change_key(config_copy, "SIZE", str(size))
        change_key(config_copy, "random.seed", str(seed))
        change_key(config_copy, "FINDMODE", str(find_mode))
        # change_key(config_copy, "TRAFFIC_STEP", traffic_step)
        # change_key(config_copy, "OBSERVER_STEP", observer_step)

        # Run the simulation
        command = f'java -Xmx200000m -cp "{classpath};{target_path}" -ea peersim.Simulator "{config_copy}" > nul 2> nul'
        os.system(command)

        # Move the generated log files to the appropriate log folder/directory
        log_dir_config = os.path.join(log_dir, f"log_{size}_{find_mode}")
        os.makedirs(log_dir_config, exist_ok=True)
        shutil.move(os.path.join(log_dir, 'count.csv'), os.path.join(log_dir_config, f"count_{size}_{find_mode}.csv"))
        shutil.move(os.path.join(log_dir, 'messages.csv'), os.path.join(log_dir_config, f"messages_{size}_{find_mode}.csv"))
        shutil.move(os.path.join(log_dir, 'operation.csv'), os.path.join(log_dir_config, f"operation_{size}_{find_mode}.csv"))
        shutil.move(os.path.join(log_dir, 'routingtable.csv'), os.path.join(log_dir_config, f"routing_table_{size}_{find_mode}.csv"))

        # Calculate the averages
        operation_file = os.path.join(log_dir_config, f"operation_{size}_{find_mode}.csv")
        stop_average, hops_average = calculate_average(operation_file)

        # Modify the operation CSV file to include the averages
        if stop_average is not None and hops_average is not None:
            modify_operation_csv(operation_file, stop_average, hops_average)

        print("Simulation completed:", config_file, "with size", size, "seed", seed, "find mode", find_mode)

    except Exception as e:
        print("Error occurred during simulation:", e)

def main() -> int:
    shutil.rmtree(output_dir, ignore_errors=True)
    shutil.rmtree(log_dir, ignore_errors=True)

    os.makedirs(output_dir, exist_ok=True)
    os.makedirs(log_dir, exist_ok=True)

  
    # traffic_steps = [36, 18, 9, 4.5, 3.6]
    # observer_steps = [35, 17, 8, 4.4, 3.5]

    for config_file in config_files:
        for size, seed in node_sizes.items():
            # for i in range(len(traffic_steps)):
                for find_mode in find_modes:
                    # Uncomment to put the change the traffic step 
                    # print("Running", config_file, "with size", size, "seed", seed, "find mode", find_mode, "traffic step", traffic_steps[i])

                    # # Update the traffic step and observer step
                    # traffic_step = traffic_steps[i]
                    # observer_step = observer_steps[i]
                    
                    # Run the simulation
                    # run_sim(config_file, size, seed, find_mode, traffic_step, observer_step)

                    print("Running", config_file, "with size", size, "seed", seed, "find mode", find_mode)
                    run_sim(config_file, size, seed, find_mode)

    return 0

if __name__ == '__main__':
    main()
