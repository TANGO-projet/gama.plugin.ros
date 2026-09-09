# Running the Gazebo models

These files support `05 Gazebo mirror.gaml` and `06 Gazebo in the loop.gaml`. The other models in this
project need nothing installed; these two need Gazebo Sim, ROS 2 and `ros_gz_bridge`.

| File | Used by | What it is |
|---|---|---|
| `gama_mirror.sdf` | model 05 | six entities driven by `VelocityControl`, no gravity, no friction |
| `gama_mirror_bridge.yaml` | model 05 | six `ROS_TO_GZ` bridges for `cmd_vel` |
| `gama_loop.sdf` | model 06 | one differential-drive robot with real physics, walls and a pillar |
| `gama_loop_bridge.yaml` | model 06 | `cmd_vel` out, `odometry` back |

## Prerequisites

```bash
sudo apt install ros-${ROS_DISTRO}-ros-gz-bridge   # Jazzy/Kilted: ros-gz is the gz-sim integration
```

Gazebo Sim (Harmonic or later) is expected. If you are on Fortress, rename the plugins in the two SDF
files: `gz-sim-*-system` becomes `ignition-gazebo-*-system` and `gz::sim::systems::` becomes
`ignition::gazebo::systems::`.

## Model 05 — GAMA simulates, Gazebo shows

Three terminals, in this order:

```bash
gz sim -r gama_mirror.sdf
```

```bash
ros2 run ros_gz_bridge parameter_bridge --ros-args -p config_file:=gama_mirror_bridge.yaml
```

Then run the `mirror` experiment in GAMA. The six boxes should start moving as soon as the first cycle
runs, following the six triangles of the GAMA display.

## Model 06 — the full loop

```bash
gz sim -r gama_loop.sdf
```

```bash
ros2 run ros_gz_bridge parameter_bridge --ros-args -p config_file:=gama_loop_bridge.yaml
```

Then run the `closed_loop` experiment. Its `init` waits up to five seconds for odometry and prints what
it found, so a setup problem is reported rather than looking like a robot that will not move.

**`gz sim -r` matters**: without `-r` the world starts paused, no odometry is published, and the model
reports that nothing publishes on `/model/gama_bot/odometry`.

## The entities keep moving after GAMA stops

Expected, and worth understanding once. `cmd_vel` carries a **setpoint, not an impulse**: `VelocityControl`
and `DiffDrive` both re-apply the last twist they received at every step of the physics, forever.
Stopping GAMA only stops the messages, and "no more messages" is indistinguishable from "the same
command still holds".

Real robots close this hole with a watchdog that zeroes the motors after a fraction of a second without
a command. Neither gz-sim system has one, so the stop has to be sent deliberately. Both experiments
carry a **Stop** user command for that, usable while they are still open; once the experiment is closed
its ROS node is gone with it and the stop has to come from a terminal:

```bash
ros2 topic pub --once /model/gama_bot/cmd_vel geometry_msgs/msg/Twist "{}"
```

`Ctrl+R` in Gazebo resets the world, which also works.

## Checking each link separately

The chain is GAMA → DDS → bridge → Gazebo, and it is worth testing one hop at a time when nothing
moves.

```bash
# Does Gazebo publish and listen where we think?
gz topic -l | grep gama

# Is the bridge exposing the ROS side?
ros2 topic list | grep gama

# Does Gazebo obey a command sent by hand? (model 06)
ros2 topic pub -r 10 /model/gama_bot/cmd_vel geometry_msgs/msg/Twist "{linear: {x: 0.4}}"

# Is odometry coming back?
ros2 topic echo /model/gama_bot/odometry --once

# Is GAMA's own traffic visible to ROS? Run model 01 and:
ros2 topic echo /chatter
```

If `ros2 topic echo /chatter` shows GAMA's messages, the DDS link is fine and the problem is in the
bridge or the SDF. If it shows nothing, the problem is between GAMA and ROS — see below.

The single most useful command when a model reports that nobody is listening:

```bash
ros2 topic info -v /model/gama_0/cmd_vel
```

It prints `Publisher count`, `Subscription count` and the full QoS of both ends. `Publisher count: 1`
with `Subscription count: 0` means GAMA is fine and no bridge is subscribed — most often a bridge that
is running happily with the *other* config file. Check that starting the mirror bridge prints **six**
`Creating ROS->GZ Bridge` lines, not two. GAMA appears as `_CREATED_BY_BARE_DDS_APP_` with
`Topic type hash: INVALID`, which is normal: jros2 is a bare DDS application, not an rclcpp node, and
it does not prevent matching.

## When GAMA and ROS cannot see each other

**`ROS_DOMAIN_ID`** must match. GAMA reads it the way ROS does; `ros_node("name", 42)` overrides it
explicitly, which is the reliable way to be sure.

**RMW implementation.** GAMA talks DDS directly through jros2, which embeds Fast DDS. ROS 2 Jazzy and
Kilted default to `rmw_fastrtps_cpp`, which interoperates. If your setup uses CycloneDDS, the safest fix
is to run the bridge under Fast DDS:

```bash
RMW_IMPLEMENTATION=rmw_fastrtps_cpp ros2 run ros_gz_bridge parameter_bridge \
    --ros-args -p config_file:=gama_loop_bridge.yaml
```

**GAMA on Windows, ROS in WSL2.** This is the case that fails silently. By default WSL2 sits behind NAT,
so DDS discovery between the Windows host and the Linux guest does not work in either direction. On
Windows 11 22H2 and later, put this in `%UserProfile%\.wslconfig` and run `wsl --shutdown`:

```ini
[wsl2]
networkingMode=mirrored
```

Then the two sides share the host's network stack and discovery works. Allowing `java.exe` through the
Windows firewall on private networks may also be needed. Running GAMA inside WSL2 alongside ROS avoids
the question entirely.

## Adding entities to model 05

The number of entities is fixed by the SDF, not by GAMA: `nb_agents` in the model cannot exceed what
`gama_mirror.sdf` declares. To add a seventh, copy the last `<model name="gama_5">` block, rename it to
`gama_6`, give it a different `<pose>`, add the matching entry to `gama_mirror_bridge.yaml`, and add its
starting x to `gz_start_x` in the GAML — the two worlds have to start from the same pose or the mirror
is only ever parallel to the original.
