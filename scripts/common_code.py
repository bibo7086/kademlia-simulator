import os
import csv

unix_config_files = ["../simulator/config/kademlia.cfg"] # List of config file paths
unix_output_dir = "../simulator/output/"  # Output directory path
unix_log_dir = "../simulator/logs/"  # Log directory path
unix_base_path = '../simulator'

unix_jar_paths = [
    os.path.join(unix_base_path, 'lib', 'djep-1.0.0.jar'),
    os.path.join(unix_base_path, 'lib', 'jep-2.3.0.jar'),
    os.path.join(unix_base_path, 'lib', 'gs-core-2.0.jar'),
    os.path.join(unix_base_path, 'lib', 'mbox2-1.0.jar'),
    os.path.join(unix_base_path, 'lib', 'gs-ui-swing-2.0.jar'),
]

target_path = os.path.join(unix_base_path, 'target', 'service-discovery-1.0-SNAPSHOT.jar')
unix_classpath = ':'.join(unix_jar_paths)


windows_config_files = ['..\\simulator\\config\\kademlia.cfg'] # List of config file paths
windows_output_dir = '..\\simulator\\output' # Output directory path
windows_log_dir = '..\\simulator\\logs' # Log directory path
windows_base_path = '..\\simulator'

windows_jar_paths = [
    os.path.join(windows_base_path, 'lib', 'djep-1.0.0.jar'),
    os.path.join(windows_base_path, 'lib', 'jep-2.3.0.jar'),
    os.path.join(windows_base_path, 'lib', 'gs-core-2.0.jar'),
    os.path.join(windows_base_path, 'lib', 'mbox2-1.0.jar'),
    os.path.join(windows_base_path, 'lib', 'gs-ui-swing-2.0.jar'),
]

windows_target_path = os.path.join(windows_base_path, 'target', 'service-discovery-1.0-SNAPSHOT.jar')
windows_classpath = ';'.join(windows_jar_paths)

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