import os
import shutil

config_files = ['..\\simulator\\config\\kademlia.cfg'] # List of config file paths
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

def main() -> int:
    shutil.rmtree(log_dir, ignore_errors=True)

    os.makedirs(log_dir, exist_ok=True)
    # Run the simulation

    try: 
        command = f'java -Xmx200000m -cp "{classpath};{target_path}" -ea peersim.Simulator {" ".join(config_files)}'
        # command = f'java -Xmx200000m -cp "{classpath};{target_path}" -ea peersim.Simulator "{config_files}"'
        os.system(command)
    
    except Exception as e:
        print("Error occurred during simulation:", e)

if __name__ == '__main__':
    main()
