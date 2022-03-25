# iFogSim - vehicleControl
## Summary
We set up a generic vehicular environment, to test the scalability of iFogSim.

## Execution
Create a eclipse workspace, and create a new project. Clone all the files from this repository to the new project directory.
Run the following file in eclipse: `${projectPath}/src/org/fog/test/perfeval/vehicleControl.java`
## Application
### Application Model
![](https://i.imgur.com/DEkZzIZ.png)

### Application Edges
| Tuple Type |  Source | Destination | CPU Length (MIPS) | Network Length (bytes) | Periodic Transmission (ms) |
| -------- | -------- | -------- | -------- | -------- |-------- |
| SENSOR | SENSOR | Client |4000 | 500 |  5 |
| CAR_CONDITION| Client | Calculator  |4500 | 500 |  NA |
| LOCAL_TRAFFIC_STATE | Calculator | Monitor  | 500 | 500 |  100 | 
| MOVEMENT_INSTRUCTION| Calculator | Client  | 1000 | 500 |  NA |
| MOVEMENT_UPDATE | Client | ACTUATOR  | 1000 | 500 |  NA |

### Tuple Mapping
|  Module | Input Tuple | Output Tuple | Fractional Selectivity |
| -------- | -------- | -------- | -------- |
| Client | SENSOR | CAR_CONDITION | 1 |
| Calculator | CAR_CONDITION | MOVEMENT_INSTRUCTION | 1 |
| Client | MOVEMENT_INSTRUCTION | MOVEMENT_UPDATE | 1 |
| Calculator | SENSOR | LOCAL_TRAFFIC_STATE | 100 ms |

## Physical Device
| Device Type | MIPS | RAM (GB) | Uplink Bandwidth (KB/Sec) | Downlink Bandwidth (KB/Sec) | level | Uplink Latency(ms)| Busy Power (watts) | Idle Power (watts) |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- |
| cloud | 44800 | 40 | 100 | 10000 | 0 | NA | 16 * 103 (1648) | 16 * 83.25 (1332) |
| proxy | 2800 | 4 | 10000 | 10000 | 1 | 100 | 107.339 | 83.4333 |
| rsu | 6000 | 2 | 10000 | 10000 | 2 | 6 | 87.53 | 82.44 |
| car | 1000 | 1 | 2000 | 1000 | 3 | 3 | 87.53 | 82.44 |


### Sensor and Actuator
| Name | Latency (ms) |
| -------- | --------  |
| carSensor | 2 |
| carActuator | 1 |


## Simulation
* 4 RSUs per Road Section
* 3 Cars per RSU
* Period of 10 second
### Cloud
* 9 Road sections takes about 3 hours (estimate)
* 
| # Road Section | Total # of Cars | Loop Delay (ms) | Network Usage (bytes) | Cloud Energy (Watts) | Proxy Energy (Watts) | Execution Time (ms) |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | 
| 1 | 12 | 232.0071656034263 | 159717.9 | 1.6184019023214351E7 | 834332.9999999987 | 5926 |
| 2 | 24 | 234.7908372465207 | 389692.3 | 1.604099234464309E7 | 834332.9999999987 | 25043 |
| 3 | 36 | 2855.299123558853 | 520022.4 | 1.6141714522988189E7 | 834332.9999999987 | 28524 |
| 4 | 48 | 3843.4591304039877 | 644518.6 | 1.631998564099253E7 | 834332.9999999987 | 31456 |
| 5 | 60 | 4300.632402668917 | 768972.5 | 1.632222641938397E7 | 834332.9999999987 | 24329 |
| 6 | 72 | 4549.576979350525 | 893402.4 | 1.6432132630375542E7 | 834332.9999999987 | 76863 |
| 7 | 84 | 4699.426972179003 | 1017751.6 | 1.6443678480370643E7 | 834332.9999999987 |  73177 |
| 8 | 96 | 4796.4435082009895 | 1141435.05 | 1.6443868503570696E7 | 834332.9999999987 | 267492 |


### Fog
* 60 Road sections: `Exception in thread "main" java.lang.OutOfMemoryError: Java heap space`
* 
| # Road Section | Total # of Cars | Loop Delay (ms) | Network Usage (Bytes) | Cloud Energy (Watts) | Proxy Energy (Watts) | Execution Time (ms) |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- |
| 1 | 12 | 19.409923154027556 | 13483.8 | 1.3336678592857135E7 | 834332.9999999987 | 4106 |
| 2 | 24 | 19.409923154023346 | 26967.6 | 1.3350448292857127E7 | 834332.9999999987 |  8558 |
| 3 | 36 | 19.409923154025616 | 40451.4 | 1.336421799285712E7 | 834332.9999999987  | 15231 | 
| 4 | 48 | 19.40992315402714 | 53935.2 | 1.3377987692857113E7 | 834332.9999999987 | 14212 |
| 5 | 60 | 19.40992315402861 | 102699 | 1.3391757392857093E7 | 834332.9999999987 | 9189 |
| 6 | 72 | 19.409923154029602 | 80902.8 | 1.3405527092857089E7 | 834332.9999999987 |22364 |
| 7 | 84 | 19.40992315403001 | 94386.6 | 1.3419296792857083E7 | 834332.9999999987 |23689 |
| 8 | 96 | 19.409923154030764 | 107870.4 | 1.3433066492857074E7 | 834332.9999999987 |25523 | 
| 10 | 120 | 19.409923154031464 | 134838.0 | 1.3460605892857067E7 | 834332.9999999987 | 19593 |
| 30 |  360 | 19.409923153982263 | 404514.0 | 1.3737294646428281E7 | 834332.9999999987 |  90363 |
| 50 | 600 | 19.4099231539703 | 674190.0 | 1.4013038503570937E7 | 834332.9999999987 | 219954 |

### Graphs
![](https://i.imgur.com/N1nszCX.png)
![](https://i.imgur.com/8OCB1tS.png)
![](https://i.imgur.com/qLJzIR5.png)



