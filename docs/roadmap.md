# TransportMod 路线图

状态：未完成（草案，等待讨论确认）。

> 本文档目前是中文讨论稿，尚未完成最终定稿。

更新日期：2026-08-12

## 1. 当前状态

- 项目基于 NeoForge MDK 初始化，mod id 为 `mtm`。
- 目标环境：Minecraft `26.1.2`，NeoForge `26.1.2.94`，Java 25。
- 已配置 `registry-lib`、JEI、Jade 依赖和 datagen 源集。
- 当前 `src/main/java` 还没有 Java 源码，`neoforge.mods.toml` 仍是模板内容。
- `docs/architecture.md` 已补充通用工程基线，交通方式细节由 `docs/roadmap/` 中的独立文档承载。

## 2. 使用方式

- 本文件定义代码实现顺序，不是玩法阶段顺序。
- 每个节点固定包含：状态、概括、玩法与实现细则、最小实现范围、文档链接。
- 对应节点细则在需要时按需读取，不要求每个实现任务都读取全部 `docs/roadmap/*`。
- 新包、公共 API 或依赖方向变化仍先按 `docs/architecture.md` 处理。

## 3. 实现顺序

### Node 0：基础工程

- 状态：`next`
- 概括：先建立可运行、可构建、包边界清晰的 mod 骨架，并搭起 KubeJS 插件基础。
- 玩法与实现细则：
  - 创建主 `@Mod` 入口类，完成 mod 初始化。
  - 整理基础包结构与依赖方向，将通用工程基线写入 `docs/architecture.md`。
  - 在 `integration` 下建立 KubeJS 插件骨架，为后续脚本事件、绑定和类型包装预留接入点。
  - 补齐基础 datagen、构建验证和测试基线。
- 最小实现范围：client 和 server 可启动，项目可构建，KubeJS 插件可被发现。
- 文档：
  - [docs/architecture.md](docs/architecture.md)
  - [docs/roadmap/kubejs.md](docs/roadmap/kubejs.md)

### Node 1：铁路

- 状态：`planned`
- 概括：实现第一个完整交通方式，以铁路货运为主，并允许玩家操控载具。
- 玩法与实现细则：
  - 实现轨道、站点、车辆和装卸货的基础玩法闭环。
  - 建立可测试的铁路领域模型与路线运行逻辑。
  - 提供玩家进入、控制和观察列车状态的基础交互。
  - 同步定义铁路系统的 KubeJS 事件、绑定和类型包装接入点。
- 最小实现范围：玩家可以布置基础设施、生成列车并完成一次货运或手动驾驶闭环。
- 文档：[docs/roadmap/rail.md](docs/roadmap/rail.md)

### Node 2：卡车

- 状态：`planned`
- 概括：在铁路之后加入道路货运，覆盖不同运力的卡车。
- 玩法与实现细则：
  - 设计不同运力档位、装载能力和道路运行规则。
  - 复用或对齐铁路阶段形成的货运、装卸货和网络公共概念。
  - 实现玩家可操控卡车以及可选的自动货运流程。
  - 同步补充卡车的 KubeJS 接入点。
- 最小实现范围：不同运力卡车可以生成、装载、行驶和卸载，并形成可操作玩法闭环。
- 文档：[docs/roadmap/truck.md](docs/roadmap/truck.md)

### Node 3：无人机空运

- 状态：`tentative`
- 概括：后续可选加入无人机空运，当前只建立占位文档和范围草案。
- 玩法与实现细则：
  - 待确认无人机的货运范围、航线和充电/维护机制。
  - 待确认无人机与铁路、卡车之间的货物转运关系。
  - 待确认无人机是否由玩家直接操控，或主要作为自动运输单位。
- 最小实现范围：暂不实施，直到文档内容讨论并确认。
- 文档：[docs/roadmap/drone.md](docs/roadmap/drone.md)

### Node 4：水运

- 状态：`tentative`
- 概括：后续可选加入水运，当前只建立占位文档和范围草案。
- 玩法与实现细则：
  - 待确认水面载具、港口和装卸货规则。
  - 待确认水运与铁路、卡车、无人机之间的连接方式。
  - 待确认水运是否作为独立交通网络或作为节点网络的扩展。
- 最小实现范围：暂不实施，直到文档内容讨论并确认。
- 文档：[docs/roadmap/water.md](docs/roadmap/water.md)

### 并行轨道：KubeJS 集成

- 状态：`next`，从 Node 0 开始持续并行推进。
- 概括：让脚本能够按需读取和操作 TransportMod 的交通能力，先开放脚本事件、全局绑定和类型包装，内容注册后置。
- 玩法与实现细则：
  - 在 `integration` 下建立 KubeJS 插件入口与插件发现配置。
  - 注册 TransportMod 专属全局绑定、事件组和类型包装。
  - 随铁路、卡车、无人机、水运节点逐步补充对应接入点。
  - 后续再讨论是否开放脚本内容注册，例如自定义车辆或交通部件。
- 最小实现范围：KubeJS 插件可加载，脚本可以触发或读取 TransportMod 的基础事件和绑定。
- 文档：[docs/roadmap/kubejs.md](docs/roadmap/kubejs.md)

## 4. 关联文档

- [docs/architecture.md](docs/architecture.md)：通用工程基线与架构决策。
- [docs/design-principles.md](docs/design-principles.md)：项目设计原则。
- [docs/roadmap/rail.md](docs/roadmap/rail.md)：铁路细则。
- [docs/roadmap/truck.md](docs/roadmap/truck.md)：卡车细则。
- [docs/roadmap/drone.md](docs/roadmap/drone.md)：无人机空运细则。
- [docs/roadmap/water.md](docs/roadmap/water.md)：水运细则。
- [docs/roadmap/kubejs.md](docs/roadmap/kubejs.md)：KubeJS 集成细则。
