# Create Nuclear Industry - Development Plan

## Project Overview
A NeoForge 1.21.1 mod that adds a fully functional nuclear reactor multiblock structure to Minecraft, with integration to the Create mod's kinetic power system. The reactor uses uranium fuel rods, control rods (movable via Create contraptions), and generates steam to drive turbines or produce rotational force.

**Mod ID:** create_nuclearindustry  
**Minecraft Version:** 1.21.1  
**Create Mod Integration:** 6.0.6+  
**Framework:** NeoForge  

---

## Architecture Overview

### 1. Core System Components

#### 1.1 Multiblock Structure System
- **Controller Block**: NuclearReactorControllerBlock (already exists with on/off state)
- **Structure Validator**: Validates multiblock formation, dimensions, block placement
- **Block Entity**: Stores reactor state, fuel inventory, temperature, power output
- **Structure Definition**: Flexible dimensions with min/max bounds

**Reactor Structure (Proposed):**
```
┌─────────────────────────────────┐
│  CONTROL ROD (movable, top)     │
│  ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼  │
│  REACTOR CASING (frame)         │
│  ┌─────────────────────────────┐│
│  │ URANIUM_ROD (fuel)          ││
│  │ URANIUM_ROD (fuel)          ││
│  │ URANIUM_ROD (fuel)          ││
│  │ URANIUM_ROD (fuel)          ││
│  │ URANIUM_ROD (fuel)          ││
│  │ URANIUM_ROD (fuel)          ││
│  │ URANIUM_ROD (fuel)          ││
│  │ URANIUM_ROD (fuel)          ││
│  │ URANIUM_ROD (fuel)          ││
│  │ HEAT_EXCHANGER (steam out)  ││
│  └─────────────────────────────┘│
│  REACTOR_CONTROLLER (center)    │
└─────────────────────────────────┘
```

**Minimum structure:** 3×3×4 (inner chamber: 1×1×2)  
**Maximum structure:** Configurable, suggest 11×11×11  

---

### 2. Nuclear Physics System

#### 2.1 Neutron & Fission Mechanics
**State Variables per Reactor:**
- `neutronLevel`: 0-1000 (represents active fission rate)
- `fuelBurnRate`: neutrons/tick consumed by uranium-235
- `controlRodEffectiveness`: How much the control rod absorbs neutrons (0-1)
- `controlRodPosition`: Height in reactor (0-1, 1=fully withdrawn, 0=fully inserted)

**Fission Formula (per tick):**
```
neutronProduction = uraniumRodCount * baseFissionRate * (1 - controlRodEffectiveness * controlRodPosition)
neutronConsumption = fuelBurnRate * uraniumRodCount
neutronLevel += (neutronProduction - neutronConsumption)
```

#### 2.2 Temperature System
**State Variables:**
- `coreTemperature`: Celsius (0-4000°C, meltdown at >3500°C)
- `coolantTemperature`: Output steam temperature
- `heatGenerationRate`: Heat produced per fission neutron
- `heatDissipationRate`: Heat loss through cooling

**Temperature Formula (per tick):**
```
heatGeneration = neutronLevel * heatGenerationRate
heatDissipation = (coreTemperature - environment) * dissipationCoefficient + (coolantFlow * cooling_efficiency)
coreTemperature += (heatGeneration - heatDissipation) / heatCapacity
```

**Temperature Effects:**
- 0-500°C: Safe, normal operation
- 500-2000°C: Yellow warning state
- 2000-3500°C: Red danger state, increased fuel depletion
- >3500°C: MELTDOWN - explosion, core damage, radiation

#### 2.3 Fuel Depletion
**Uranium-235 Consumption:**
- Base consumption rate tied to neutron level
- Depletion accelerates with temperature (exponential above 2000°C)
- Fuel completely consumed → reactor stops producing heat
- Can be refueled via control interface

---

### 3. Integration with Create Mod

#### 3.1 Control Rod Movement
**System:**
- Control rod is a **Kinetic Source** that accepts rotational force from Create contraptions
- Piston or Mechanical Arm can push/pull the control rod vertically
- Speed of rotation maps to withdrawal rate (negative speed = insertion)
- Position ranges from 0 (fully inserted) to 1 (fully withdrawn)

**Implementation:**
- BlockEntity extends create.foundation.blockEntity.SmartBlockEntity
- Implements `IRotationSource` for kinetic integration
- Rotation speed (RPM) maps to rod position change per tick
- Safe RPM limits to prevent dangerous rapid withdrawal

#### 3.2 Steam Output & Turbine Integration
**System:**
- Heat Exchanger block outputs STEAM fluid (already implemented)
- Steam production rate = `(coreTemperature - 373K) * efficiency`
- Steam flows to turbine(s) via fluid pipes
- Turbine converts steam pressure/flow into rotational force

**Implementation Options:**
- **Option A (Steam Turbine):** Create custom turbine block that converts steam into rotational force
- **Option B (Direct Torque):** Heat exchanger block directly outputs rotational force
- **Recommendation:** Option A (more authentic, better gameplay)

**Turbine Block Design:**
- Takes steam input from pipes
- Outputs rotational force based on steam pressure/temperature
- Formula: `torque = steamFlow * (steamTemp - 373K) / constantFactor`
- Can be modulated with intake valves

#### 3.3 Create Recipe Integration
**Current recipes:**
- Raw Uranium → Uranium-238 (mixing)
- Uranium-238 → Uranium-235 (5 cycles, enrichment process)
- Borax Salt → Boron (splashing)

**New recipes to add:**
- Boron + Uranium → Boron-infused Uranium-235 (creates control rod material)
- Uranium-235 pellets → Refined Uranium (smelting/compacting)
- Custom recipe for spent fuel rod processing

---

### 4. Block Entities & Data Persistence

#### 4.1 ReactorBlockEntity (for REACTOR_CONTROLLER)
**Extends:** SmartBlockEntity (from Create)  
**Responsibilities:**
- Store reactor state (neutronLevel, temperature, fuel inventory, etc.)
- Manage multiblock structure (validate on placement/neighbor update)
- Tick-based simulation of physics
- Handle fuel/coolant fluid transfers
- Manage control rod position and kinetic source

**Key Methods:**
```java
public class ReactorBlockEntity extends SmartBlockEntity {
    // State
    private float coreTemperature;
    private float neutronLevel;
    private int[] fuelInventory;  // inventory for uranium rods
    private float controlRodPosition;
    
    // Structure
    private boolean isFormed;
    private Set<BlockPos> structureBlocks;
    
    // Kinetic
    private KineticNetwork network;
    
    // Methods
    void validateStructure()
    boolean tryFormMultiblock()
    void tick()
    void updateTemperature()
    void updateNeutrons()
    void consumeFuel()
    float getControlRodEffectiveness()
    void setControlRodPosition(float newPosition)
    float getEnergy()
    void transferSteam(FluidStack steam)
}
```

**NBT Serialization:**
- Store: temperature, neutronLevel, fuel amounts, control rod position, isFormed flag
- Load: restore state on chunk load

#### 4.2 ControlRodBlockEntity (optional, for detailed rod tracking)
**Extends:** BlockEntity  
**Responsibilities:**
- Track individual control rod position
- Sync position to client for rendering
- Handle collisions/safety limits

#### 4.3 HeatExchangerBlockEntity (optional, for fluid transfer)
**Extends:** SmartBlockEntity  
**Responsibilities:**
- Manage steam output based on reactor temperature
- Handle fluid transfers via pipes
- Throttle steam production with backpressure

---

### 5. GUI & Control Interface

#### 5.1 Reactor Control Panel (GUI)
**Screen Class:** ReactorControlScreen  
**Widget Types:**
- **Reactor Status Display:**
  - Core Temperature gauge (0-4000°C with color gradient)
  - Neutron Level bar (0-1000)
  - Power Output gauge (MW or Create units)
  - Reactor State indicator (IDLE/RUNNING/CRITICAL/MELTING)
  
- **Fuel Management:**
  - Uranium-235 inventory (amount remaining, percentage)
  - Fuel depletion rate indicator
  - Refuel button to insert new fuel
  
- **Control Systems:**
  - Control Rod Position slider (0-100%)
  - Manual Mode / Automated Mode toggle
  - Auto-shutdown at temperature threshold
  - Emergency Shutdown button (RED, LARGE)
  
- **Coolant System:**
  - Coolant flow rate display
  - Steam output pressure gauge
  - Connection status for turbines

**Network Packet:**
- ReactorUpdatePacket: Send reactor state to client (20Hz update)
- ControlRodPositionPacket: Client sends manual rod adjustments

#### 5.2 Turbine Control (if using turbine block)
**Screen Class:** TurbineControlScreen  
**Display:**
- Steam input pressure
- Rotational output (RPM)
- Efficiency percentage
- Temperature gauge

---

### 6. Safety & Hazard Systems

#### 6.1 Meltdown Mechanics
**Trigger:** Core temperature > 3500°C for 200 ticks  
**Effects:**
1. Block particle effects (smoke, sparks)
2. EXPLOSION animation and sound
3. Core blocks become DAMAGED_REACTOR_CORE (new block type)
4. Damage nearby blocks and entities
5. Creates radiation zone for 30 blocks radius
6. Spreads fire to adjacent flammable blocks
7. Reactor becomes inoperable, must be rebuilt

#### 6.2 Radiation System (Optional Advanced Feature)
**On Meltdown:**
- Creates radiation particles
- Players in zone take increasing damage over time
- Requires lead armor or anti-radiation items to survive
- Radiation slowly decays over time (1 hour to clear)

#### 6.3 Pressure System (Future)
- Coolant backup creates pressure
- Pressure venting mechanism (automatic relief valve)
- Overpressure can damage reactor structure

---

### 7. Configuration System

**Config Parameters (in Config.java):**
```java
// Physics
static double BASE_FISSION_RATE = 10.0;  // neutrons per uranium rod per tick
static double HEAT_GENERATION_RATE = 0.1;  // °C per neutron
static double HEAT_DISSIPATION_RATE = 0.5;  // °C per tick (passive)
static double CONTROL_ROD_EFFECTIVENESS = 0.8;  // neutron absorption rate

// Safety
static int MELTDOWN_TEMPERATURE = 3500;  // °C
static int MELTDOWN_TICKS = 200;  // ticks before explosion
static double MELTDOWN_EXPLOSION_POWER = 3.0;  // blast power

// Structure
static int REACTOR_MIN_WIDTH = 3;
static int REACTOR_MIN_HEIGHT = 4;
static int REACTOR_MAX_WIDTH = 11;
static int REACTOR_MAX_HEIGHT = 11;

// Power Output
static int POWER_MULTIPLIER = 100;  // watts per neutron level
static double STEAM_GENERATION_RATE = 0.5;  // mB per tick per °C above 373K
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1)
**Goal:** Establish reactor multiblock structure and basic physics

1. **1.1 Create ReactorBlockEntity**
   - Extend SmartBlockEntity
   - Add state fields (temperature, neutronLevel, fuel inventory)
   - Implement NBT save/load
   - Register as BlockEntity type

2. **1.2 Implement Multiblock Validation**
   - Create MultiblockValidator class
   - Check structure dimensions and block composition
   - Generate structure bounds on successful formation
   - Add isFormed flag to BlockEntity
   - Handle neighbor updates to re-validate

3. **1.3 Basic Physics Simulation**
   - Implement tick() method with temperature/neutron update
   - Add simple fission formula (without control rod modulation yet)
   - Implement fuel consumption
   - Add temperature increase/decrease

4. **1.4 Update REACTOR_CONTROLLER Block**
   - Make it reference the BlockEntity
   - Add visual feedback for reactor state (particles when running)
   - Add click interaction to open GUI

---

### Phase 2: Control & Interaction (Week 2)
**Goal:** Add control rod movement and reactor interface

1. **2.1 Create Reactor Control GUI**
   - Build screen with gauges and displays
   - Display temperature, neutron level, fuel status
   - Add network packets for state sync

2. **2.2 Implement Control Rod Kinetic Integration**
   - Make ControlRodBlock accept rotational force from Create
   - Implement IRotationSource in ReactorBlockEntity
   - Map RPM to rod position change
   - Add safe RPM limits

3. **2.3 Control Rod Physics**
   - Control rod position affects fission rate (via effectiveness)
   - Update neutron formula to include rod position
   - Add visual indicators of rod position

4. **2.4 Safety Features**
   - Implement meltdown detection
   - Add emergency shutdown mechanism
   - Add visual/audio warnings at high temperature
   - Implement block damage on meltdown

---

### Phase 3: Power Generation (Week 3)
**Goal:** Add steam output and Create integration

1. **3.1 Steam Production System**
   - Calculate steam generation rate based on core temperature
   - Update Heat Exchanger to output steam fluid
   - Handle fluid transfers via fluid networks

2. **3.2 Create Turbine Block (OPTION A)**
   - Design new block for steam turbine
   - Accept steam input
   - Convert steam pressure/temperature to rotational force
   - Create kinetic output
   - Add models and textures

   **OR Option B: Direct Torque Output**
   - Make Heat Exchanger output rotational force directly
   - Simpler but less elegant

3. **3.3 Create Recipe Integration**
   - Add mixing recipe for boron + uranium → control rod material
   - Add compacting recipe for fuel pellet assembly
   - Balance recipes for gameplay

4. **3.4 Power Balancing**
   - Calculate realistic power output (watts/Create units)
   - Balance fuel consumption vs. power generation
   - Test with Create machines (e.g., pump, grinder)

---

### Phase 4: Polish & Advanced Features (Week 4)
**Goal:** Add advanced features and optimize

1. **4.1 Fuel Management System** (Optional)
   - Fuel rod slots in GUI
   - Add/remove fuel rods interface
   - Spent fuel rod outputs
   - Fuel depletion percentages

2. **4.2 Advanced Physics** (Optional)
   - Plutonium breeding (convert some U-238 to Pu-239)
   - Pressure system with relief valves
   - Xenon poisoning (temporary fission suppression)

3. **4.3 Cosmetic Enhancements**
   - Particle effects for various reactor states
   - Sound effects (humming, alarm, explosion)
   - Animated models for control rod
   - Smooth temperature gauge animations

4. **4.4 Configuration & Balancing**
   - Implement all config options
   - Test and balance power output
   - Adjust heat generation rates
   - Fine-tune control rod responsiveness

5. **4.5 Documentation**
   - In-game tooltips for reactor blocks
   - Advancement/achievement system
   - Create guidebook integration (if available)

---

## Technical Implementation Details

### Key Classes to Create

```
org.papiricoh.create_nuclearindustry/
├── blockentity/
│   ├── ReactorBlockEntity.java              [PRIORITY 1]
│   ├── ControlRodBlockEntity.java           [PRIORITY 3]
│   └── HeatExchangerBlockEntity.java        [PRIORITY 3]
├── multiblock/
│   └── ReactorStructureValidator.java       [PRIORITY 1]
├── physics/
│   └── ReactorPhysicsSimulator.java         [PRIORITY 1]
├── gui/
│   ├── ReactorControlScreen.java            [PRIORITY 2]
│   ├── ReactorControlMenu.java              [PRIORITY 2]
│   └── widgets/
│       ├── TemperatureGaugeWidget.java
│       ├── NeutronLevelWidget.java
│       └── ControlRodSlider.java
├── kinetic/
│   └── ControlRodKineticSource.java         [PRIORITY 2]
└── blocks/
    ├── HeatExchangerBlock.java              [UPDATE]
    ├── TurbineBlock.java                    [PRIORITY 3, optional]
    └── DamagedReactorCoreBlock.java         [PRIORITY 2]

org.papiricoh.create_nuclearindustry.blocks/ (existing)
└── Update existing block classes with proper integration
```

### Key Interfaces/Mixins
- `IRotationSource` (Create API): For kinetic power output
- `IHaveGoggleInformation` (Create API): For goggle tooltip support
- `IRotationSourceProvider` (Create API): For control rod to act as kinetic source

### Key Dependencies
- Create mod (already included)
- NeoForge BlockEntity and networking APIs
- Minecraft FluidStack and fluid networking

---

## Development Guidelines

### Code Quality
- Follow existing naming conventions (SCREAMING_SNAKE_CASE for constants)
- Use DeferredRegister/DeferredHolder for all registrations
- Implement proper NBT serialization for all state
- Use SmartBlockEntity from Create for easier syncing

### Physics Accuracy (Gamified)
- Reactor physics simplified for gameplay (not realistic nuclear sim)
- Balance power output vs. fuel consumption for interesting gameplay
- Make control rod movement feel responsive and important
- Meltdown should be spectacular but avoidable with skill

### Testing Checklist
- [ ] Multiblock validates with correct structure
- [ ] Multiblock rejects invalid structures (wrong blocks, gaps, etc.)
- [ ] Reactor starts and maintains fission on its own (no rod needed)
- [ ] Control rod effectively reduces neutron level when inserted
- [ ] Control rod responds to Create contraption pushing
- [ ] Temperature rises and falls realistically
- [ ] Steam production increases with temperature
- [ ] Turbine generates rotational force proportional to steam
- [ ] Meltdown triggers at correct temperature
- [ ] Player can prevent meltdown with skilled control rod management
- [ ] Fuel depletes over time
- [ ] GUI displays all values correctly
- [ ] Network sync works across multiplayer

---

## Minecraft Wiki & Reference Materials

### Useful Vanilla Systems to Study
- Furnace block entity (temperature simulation example)
- Cauldron fluid system
- Brewing stand (multi-step process)

### Create Mod Systems to Learn
- SmartBlockEntity for syncing
- Kinetic network system
- FluidStack and fluid pipes
- Create recipes (Mixing, Splashing)
- Mechanical Power (RPM, stress, torque)

### Similar Mods (For Inspiration)
- Gregtech/Immersive Engineering (nuclear reactors)
- ModularMachinery (multiblock validation)
- Ender IO (conduit systems)

---

## Known Challenges & Solutions

| Challenge | Solution |
|-----------|----------|
| Control rod collision with other blocks | Use BlockStateBase with custom VoxelShape |
| Network sync of real-time values | Use SmartBlockEntity auto-syncing + update packets |
| Fluid backpressure handling | Implement fluid backpressure check in steam output |
| Player accidentally breaks multiblock | Add "multiblock formed" state check before breaking |
| Kinetic network integration | Follow Create's IRotationSource pattern |
| Performance with many reactors | Batch updates, only simulate active reactors |

---

## Future Expansion Ideas
- Multiple reactor types (fast breeder, CANDU, pebble bed)
- Radiation suits and anti-radiation upgrades
- Fuel rod crafting/assembly line
- Coolant variants (water, liquid sodium, molten salt)
- Backup diesel generators
- Control room console block
- Create mechanical arm automatic rod control
- Plutonium fuel cycle
- Waste processing facility
- Integrated automation system

