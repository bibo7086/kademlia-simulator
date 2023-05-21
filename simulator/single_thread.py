import sys
import os
from shutil import copyfile

result_dir = "/simulation/logs/"
# Define config_files
config_files = {
    "kademlia": '/Users/machine/Documents/OneDrive - City, University of London/Documents/Development/City/kademlia-simulator/simulator/config/kademlia.cfg',
    # Add more protocols and their config paths as needed
}

# Define features
features = {
    "uniqueNodeID": {
        "type": "benign",
        "vals": [1],
        "keyword": "init.1uniqueNodeID",
        "default": 1,
        "defaultAttack": 10
    },
    "statebuilder": {
        "type": "benign",
        "vals": [1],
        "keyword": "init.2statebuilder",
        "default": 1,
        "defaultAttack": 10
    },
    "traffic": {
        "type": "benign",
        "vals": [1],
        "keyword": "control.0traffic",
        "default": 1,
        "defaultAttack": 10
    },
    "observer": {
        "type": "benign",
        "vals": [1],
        "keyword": "control.3",
        "default": 1,
        "defaultAttack": 10
    }
    # Add more features as needed
}

def change_key(file, key, val):
    # Make sure we don't overwrite an original config file
    assert(file not in config_files.values())
    if type(key) is list:
        for k in key:
            regex = "\"s@ ^" + k + " .*@" + k + " " + str(val) + "@g\""
            result = os.system("sed -i " + regex + " " + file)
            # Make sure the command succeeded
            assert(result == 0)
    else:
        regex = "\"s@^" + key + " .*@" + key + " " + str(val) + "@g\""
        result = os.system("sed -i " + regex + " " + file)
        # Make sure the command succeeded
        assert(result == 0)

def run_sim(config_file):
    result = os.system("java -Xmx200000m -cp ./lib/djep-1.0.0.jar:lib/jep-2.3.0.jar:target/service-discovery-1.0-SNAPSHOT.jar:lib/gs-core-2.0.jar:lib/pherd-1.0.jar:lib/mbox2-1.0.jar:lib/gs-ui-swing-2.0.jar -ea peersim.Simulator " + config_file + "> /dev/null 2> /dev/null")
    assert(result == 0)

def params_to_dir(params, feature_type):
    result = ""
    for param in params:
        if features[param]['type'] == feature_type:
            result += "_" + param + "-" + str(params[param])
    return result

def set_params(config_file, out_dir, params):
    os.system("dos2unix " + config_file)
    change_key(config_file, "control.3.rangeExperiment", out_dir)
    for param in params:
        key = features[param]['keyword']
        value = params[param]
        change_key(config_file, key, value)

def main():
    result_dir = "results"  # Change this to your desired result directory
    os.system('rm -rf ' + result_dir)

    config_folder = "config"  # Folder containing configuration files

    for protocol in config_files.keys():
        print("Running", protocol)
        in_config = os.path.join(config_folder, protocol + ".cfg")
        already_run = set()
        params = {}
        for main_feature in features.keys():
            if features[main_feature]['type'] != 'benign':
                continue

            for val in features[main_feature]['vals']:
                params[main_feature] = val
                for feature in features.keys():
                    if feature != main_feature:
                        params[feature] = features[feature]['default']

                if str(params) not in already_run:
                    already_run.add(str(params))
                    out_dir = os.path.join(result_dir, "benign", protocol, params_to_dir(params, feature_type='benign'))
                    os.makedirs(out_dir, exist_ok=True)
                    out_config = os.path.join(out_dir, 'config.cfg')
                    copyfile(in_config, out_config)
                    set_params(out_config, out_dir, params)
                    # run_sim(out_config)

if __name__ == '__main__':
     # Change the current working directory to the parent directory (simulator)
    root_directory = os.path.dirname(os.getcwd())
    os.chdir(root_directory)

    sys.exit(main())
