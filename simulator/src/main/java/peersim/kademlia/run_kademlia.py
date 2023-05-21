import math
import simulationLauncher as launcher

config_template = """
# Network size
SIZE {size}

# Random seed
K {seed}

MINDELAY  100
MAXDELAY  100

#Simulation time in ms
SIM_TIME 1000*60*60

#Traffic generator is executed every TRAFFIC_STEP
TRAFFIC_STEP  300000 #10000000/SIZE
#Tracing module is executed every OBSERVER_STEP
OBSERVER_STEP 100000
#Turbulence module is executed every TURBULENCE_STEP enabling churning
TURBULENCE_STEP  (SIM_TIME*20)/SIZE   #100000000/SIZE


# add network config parameters to simulation
random.seed {seed}
simulation.experiments 4
simulation.endtime SIM_TIME
network.size SIZE


# Peersim  protocols enabled in each node

#A protocol that stores links. It does nothing apart from that. Use by default
protocol.0link peersim.core.IdleProtocol

#A protocol that stores links. It does nothing apart from that. Use by default
protocol.1uniftr peersim.transport.UniformRandomTransport
{{
    mindelay MINDELAY
    maxdelay MAXDELAY
}}

#transport layer that reliably delivers messages with a random delay, emulating TCP
protocol.2unreltr peersim.transport.UnreliableTransport
{{
    drop 0
    transport 1uniftr
}}

#Kademlia protocol with 256 bits identifiers and 17 buckets in the routing table.
#Use FINDMODE 1 to send FINDMODE messages looking for distance to specific node instead of sending the id of the node like in DEVP2P
protocol.3kademlia peersim.kademlia.KademliaProtocol
{{
    transport 2unreltr
    BITS 256
    NBUCKETS 256
    FINDMODE 1
}}

# ::::: INITIALIZERS :::::
# Class that initializes nodes with kademlia protocol and generates uniform ids
init.1uniqueNodeID peersim.kademlia.CustomDistribution
{{
    protocol 3kademlia
}}

#Adds initial state to the routing tables
init.2statebuilder peersim.kademlia.StateBuilder
{{
    protocol 3kademlia
    transport 2unreltr
}}

# ::::: CONTROLS :::::

#TrafficGenerator class sends and initial 
control.0traffic peersim.kademlia.TrafficGenerator
{{
    protocol 3kademlia
    step TRAFFIC_STEP
}}


# ::::: OBSERVER :::::
#The observer is executed every OBSERVER_STEP and will generate data traces 
control.3 peersim.kademlia.KademliaObserver
{{
    protocol 3kademlia
    step OBSERVER_STEP
}}
"""

parameters = [
    {"size": 128, "seed": 123456789},
    {"size": 256, "seed": 67890},
    {"size": 512, "seed": 45678},
    {"size": 1024, "seed": 98765},
    {"size": 2048, "seed": 54321},
    {"size": 4096, "seed": 24680},
    {"size": 8192, "seed": 13579},
    {"size": 16384, "seed": 55555},
    {"size": 32768, "seed": 88888},
    {"size": 65536, "seed": 22222}
]

for params in parameters:
    size = params["size"]
    seed = params["seed"]

    config = config_template.format(size=size, seed=seed)
    with open(f"config_{size}_{seed}.txt", "w") as file:
        file.write(config)
        
    launcher.parse_args()
    launcher.set_expe_name_from_file_name("(.*)KFN-(.*).py", __file__, "")
    launcher.set_config_factory(lambda args, seed: config_template.format(size=args["size"], seed=seed))
    launcher.launch_single(params)