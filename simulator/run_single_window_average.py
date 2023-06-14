import os
import shutil
import csv

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
    5000: 654654, 
}

find_modes = [1,2,3]

config_files = ['.\\config\\kademlia.cfg'
]

output_dir = '.\\output'

log_dir = '.\\logs'

base_path = '.\\'

test_path = '.\\logs'

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

def calculate_average(file):
    with open(file, 'r') as f:
        reader = csv.DictReader(f)
        stop_sum = 0
        hops_sum = 0
        count = 0
        for row in reader:
            stop_sum += int(row['stop'])
            hops_sum += int(row['hops'])
            count += 1

    if count > 0:
        stop_average = round(stop_sum / count, 3)
        hops_average = round(hops_sum / count, 3) 
        return stop_average, hops_average
    else:
        return None, None

def modify_operation_csv(file, stop_average, hops_average):
    temp_file = file + '.tmp'

    with open(file, 'r') as f, open(temp_file, 'w', newline='') as temp:
        reader = csv.DictReader(f)
        writer = csv.DictWriter(temp, fieldnames=reader.fieldnames)
        writer.writeheader()
        for row in reader:
            writer.writerow(row)

    os.remove(file)
    os.rename(temp_file, file)

    with open(file, 'a', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['average', stop_average, hops_average])

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

        # Run the simulation
        command = f'java -Xmx200000m -cp "{classpath};{target_path}" -ea peersim.Simulator "{config_copy}" > nul 2> nul'
        os.system(command)

        # Move the generated log files to the appropriate log folder/directory
        log_dir_config = os.path.join(log_dir, f"log_{size}_{find_mode}")
        os.makedirs(log_dir_config, exist_ok=True)
        shutil.move(os.path.join(test_path, 'count.csv'), os.path.join(log_dir_config, f"count_{size}_{find_mode}.csv"))
        shutil.move(os.path.join(test_path, 'messages.csv'), os.path.join(log_dir_config, f"messages_{size}_{find_mode}.csv"))
        shutil.move(os.path.join(test_path, 'operation.csv'), os.path.join(log_dir_config, f"operation_{size}_{find_mode}.csv"))

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

    for config_file in config_files:
        for find_mode in find_modes:
            for size, seed in node_sizes.items():
                print("Running", config_file, "with size", size, "seed", seed, "find mode", find_mode)
                run_sim(config_file, size, seed, find_mode)

    return 0

if __name__ == '__main__':
    main()
