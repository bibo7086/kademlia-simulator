import os
import shutil
import csv 
from multiprocessing import Pool, cpu_count
from common_code import *

# Configuration parameters
node_sizes = {
    # 128: 123456789,
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
    10000: 974323, 
}

find_modes = [0, 3]  # List of find modes
    
def run_sim(config_file, size, seed, find_mode, traffic_step, observer_step):
    try:
        # Get the base name of the config file 
        config_basename = os.path.basename(config_file)

        # Create a separate directory for each configuration
        config_name = os.path.splitext(os.path.basename(config_file))[0]
        output_dir_config = os.path.join(windows_output_dir, config_name)
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
        command = f'java -Xmx200000m -cp "{windows_classpath};{windows_target_path}" -ea peersim.Simulator "{config_copy}" > nul 2> nul'
        os.system(command)

        # Move the generated log files to the appropriate log folder/directory
        log_dir_config = os.path.join(windows_log_dir, f"log_{size}_{find_mode}")
        os.makedirs(log_dir_config, exist_ok=True)

        shutil.move(os.path.join(windows_log_dir, f'count_{size}_{traffic_step}_{find_mode}.csv'), os.path.join(log_dir_config, f"count_{size}_{find_mode}_{traffic_step}.csv"))
        # shutil.move(os.path.join(windows_log_dir, f'messages_{traffic_step}_{find_mode}.csv'), os.path.join(log_dir_config, f"messages_{size}_{find_mode}_{traffic_step}.csv"))
        shutil.move(os.path.join(windows_log_dir, f'operation_{size}_{traffic_step}_{find_mode}.csv'), os.path.join(log_dir_config, f"operation_{size}_{find_mode}_{traffic_step}.csv"))
        # shutil.move(os.path.join(windows_log_dir, 'routingtable_{observer_step}.csv'), os.path.join(log_dir_config, f"routing_table_{size}_{find_mode}_{traffic_step}.csv"))

        print("Simulation completed:", config_file, "with size", size, "seed", seed, "find mode", find_mode)

    except Exception as e:
        print("Error occurred during simulation:", e)

def run_sim_wrapper(args):
    return run_sim(*args)   

def main() -> int:
    shutil.rmtree(windows_output_dir, ignore_errors=True)
    shutil.rmtree(windows_log_dir, ignore_errors=True)

    os.makedirs(windows_output_dir, exist_ok=True)
    os.makedirs(windows_log_dir, exist_ok=True)
    
    # traffic_steps = [14400, 7200, 3600, 1200, 600, 300]
    # observer_steps = [14399, 7199, 3599, 1199, 599, 299]
    traffic_steps = [1000]
    observer_steps = [1000]

    sim_args = []

    # Create a pool of worker processes
    # num_cores = cpu_count()
    # pool = Pool(5)

    num_cores = 2
    pool = Pool(num_cores)
    
    for config_file in windows_config_files:
        for size, seed in node_sizes.items():
            for i in range(len(traffic_steps)):
                for find_mode in find_modes:
                    print("Running", config_file, "with size", size, "seed", seed, "find mode", find_mode)

                    # Update the traffic step and observer step
                    traffic_step = traffic_steps[i]
                    observer_step = observer_steps[i]
                
                    # Append the simulation arguments to the list
                    sim_args.append((config_file, size, seed, find_mode, traffic_step, observer_step))

    # Run the simulations using the worker processes in parallel
    pool.map(run_sim_wrapper, sim_args)

    # Close the pool of worker processes
    pool.close()
    pool.join()
    return 0

if __name__ == '__main__':
    main()