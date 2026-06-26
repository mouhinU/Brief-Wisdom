-- ============================================
-- 简历数据表 DDL + 初始化数据
-- 数据来源: project.json
-- ============================================

USE brief_wisdom;

-- ============================================
-- 1. 工作经历表 (work_experience)
-- ============================================
DROP TABLE IF EXISTS project_achievement;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS work_experience_stack;
DROP TABLE IF EXISTS work_experience;

CREATE TABLE work_experience (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    title VARCHAR(500) NOT NULL COMMENT '职位标题（如：不动产数字化平台 | 资深技术专家）',
    job VARCHAR(200) NOT NULL COMMENT '岗位角色（如：项目Leader / 核心研发）',
    description TEXT COMMENT '整体描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    is_visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示: 1-显示, 0-隐藏',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_sort_order (sort_order),
    INDEX idx_is_visible (is_visible),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作经历表';

-- ============================================
-- 2. 项目表 (project)
-- ============================================
CREATE TABLE project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    experience_id BIGINT NOT NULL COMMENT '关联 work_experience.id',
    name VARCHAR(200) NOT NULL COMMENT '项目名称',
    lifecycle VARCHAR(100) COMMENT '项目周期（如：2024.12 - 2025.10）',
    background TEXT COMMENT '项目背景',
    duty TEXT COMMENT '职责描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_experience_id (experience_id),
    INDEX idx_sort_order (sort_order),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- ============================================
-- 3. 项目成果表 (project_achievement)
-- ============================================
CREATE TABLE project_achievement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    project_id BIGINT NOT NULL COMMENT '关联 project.id',
    content TEXT NOT NULL COMMENT '成果内容',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成果表';

-- ============================================
-- 4. 技术栈表 (work_experience_stack)
-- ============================================
CREATE TABLE work_experience_stack (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    experience_id BIGINT NOT NULL COMMENT '关联 work_experience.id',
    tech_name VARCHAR(100) NOT NULL COMMENT '技术名称',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_experience_id (experience_id),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作经历技术栈表';

-- ============================================
-- 5. 初始化数据 - 工作经历
-- ============================================

-- 工作经历 1: 不动产数字化平台
INSERT INTO work_experience (id, title, job, description, sort_order, is_visible) VALUES
(1, '不动产数字化平台 | 资深技术专家 | 阿里巴巴集团 | 武汉佰钧成技术有限责任公司',
 '项目Leader / 核心研发 /架构设计',
 '依托REOS平台化的基础能力,主导并逐步统筹工程管理、设计管理、平台任务、审批流、供应商管理、成本采购等多个关键业务线的系统建设。负责从业务需求分析、系统架构设计到核心业务攻坚的全过程,组织并协调多团队资源,推动产品落地与持续优化。建立并主导团队Code Review机制,提升代码质量和团队整体技术水平。通过引入新的技术架构和优化数据处理流程,保障核心系统稳定运行,持续优化系统性能和用户体验,确保系统能够适应不断变化的业务需求和客户期望。',
 1, 1);

-- 工作经历 2: 智慧园区
INSERT INTO work_experience (id, title, job, description, sort_order, is_visible) VALUES
(2, '智慧园区 | 技术专家 | 阿里巴巴集团 | 武汉佰钧成技术有限责任公司',
 '核心研发 / 项目Leader',
 '参与并主导多个从0到1的智慧园区核心项目的系统实现,系统的需求分析、系统架构设计及团队指导。优化系统性能,确保系统能够在高负载下稳定运行,并持续优化系统性能和用户体验,满足客户需求。',
 2, 1);

-- 工作经历 3: 早期项目经历
INSERT INTO work_experience (id, title, job, description, sort_order, is_visible) VALUES
(3, '早期项目经历| JAVA高级软件工程师  | 方正国际软件(武汉)有限公司',
 '全栈/服务开发',
 '保障每日新闻、朝日新闻,Epoch贩卖店等多个业务系统的稳定运行,并不断优化和重构相关的系统,提升用户的满意度和执行效率。',
 3, 1);

-- ============================================
-- 6. 初始化数据 - 项目
-- ============================================

-- === 工作经历 1 的项目 ===
INSERT INTO project (id, experience_id, name, lifecycle, background, duty, sort_order) VALUES
(1, 1, '设计空间改造', '2024.12 - 2025.10',
 '旨在保障园区租赁业务上的效率和提升用户的体验,给客户带来不一样的视觉体验,需要为业务解决几个核心问题:1、满足展现可租赁空间的状态信息同时需要满足客户的个性化设计定制需求的实时演示,2、企业内部图纸的可追溯在线管理问题,3、整合现有的设计管理能力实现企业内部的部署和使用,4、产品层面引入虚幻引擎和AI能力提升设计空间的视觉效果和交互体验',
 '1、通过引入新的技术架构和优化数据处理流程,协调资源推动项目落地,2、保障核心系统稳定运行,3、持续优化系统性能和用户体验,4、解决图纸版本的一致性问题,提升设计与现场管理的效率和质量',
 1),

(2, 1, '成本采购系统、供应商管理系统', '2023.06 - 2025.10',
 '本项目旨在支持置业成本采购业务,通过数字化手段提升园区成本采购和供应商管理的效率与合规性。系统涵盖了供应商管理、成本采购管理、合同管理、审批流程等核心能力,支持多业务场景下的采购需求处理和供应商协同的一套综合解决方案',
 '1、主导系统的整体架构升级,并重构核心业务,提升了系统处理高并发场景下的稳定性和可用性。2、针对历史系统不足进行深度重构与优化,解决了多个性能瓶颈,使系统响应时间降低70%。3、从扩展性、安全性角度提出改进方案,提升了代码质量和团队整体技术水平。4、主动协调产品、测试及业务方,从客户视角优化产品流程,提升用户满意度。',
 2),

(3, 1, '设计管理系统、审批流系统', '2022.07 - 2023.06/2025.10',
 '旨在解决设计、工程与现场管理图纸和文件一致性问题,整合文件管理、项目管理、任务管理等功能并实现图纸在线审图能力 ,需要提升设计与现场管理的质量与效率问题',
 '1、依托OSS文件存储的基础能力,实现图纸、文件的版本化控制,确保设计与现场管理的一致性,用以提升图纸设计与现场管理的沟通效率。2、整合BIMFace图纸解析引入在线审图功能,实现了图纸的在线审阅和协同,提升了设计与现场管理的沟通效率和溯源能力。3、持续优化系统性能和用户体验,确保系统能够适应不断变化的业务需求和客户期望。4、文件服务作为核心能力提供给其他业务线使用,推动了系统的广泛应用和价值最大化',
 3),

(4, 1, '工程管理系统', '2020.01 - 2022.06',
 '旨在解决工程的现场管理问题(质量、安全、风险、进度),通过数字化手段提升工地现场管理的效率与质量,业务涵盖了巡检、飞检、视频监控、承接查验、桩基管理、材料入场管理等核心系统能力,支持多业务场景下的现场问题处理和协同的一套综合解决方案',
 '1、深入业务一线,收集并解决工程现场管理中的核心痛点难点,基于MVP开发模式快速响应客户需求 2、带领团队攻克了复杂业务流程下的技术难题,解决弱网、无网环境下的业务系统技术难关。3、负责核心应用模块(如巡检、飞检、承接查验、桩基、材料入场管理)的设计与开发,确保了系统在高负载下的高性能运行。4、并持续推动系统因业务复杂设计不足的重构,利用Redis和MongoDB文档存储优化数据读写模型替代传统关系型数据库,大幅提升了现场复杂业务处理和响应。',
 4);

-- === 工作经历 2 的项目 ===
INSERT INTO project (id, experience_id, name, lifecycle, background, duty, sort_order) VALUES
(5, 2, '物业管理系统', '2018.05 - 2019.12',
 '为了更好的园区管理,因商业管理方案中外采的物业管理系统在多方评估下不满足多组户多园区复杂业务下的开发维护,针对变化的工单管理,设备管理,能耗管理等核心业务模块,从0到1构建了一个全新的物业管理系统,支持多园区多租户的复杂业务场景',
 '1、参与系统架构评审,引入领域驱动设计(DDD)理念进行系统重写。2、承担核心功能代码和框架的编写和维护,3、随着系统演进,主导了对系统架构上的补充与优化,持续提升系统可用性与稳定性。4、有效辅导新进开发人员,帮助他们快速掌握业务和技术栈,融入团队并产出价值。5、积极响应线上问题,并推动问题的快速解决和经验的沉淀,保障了系统稳定运行。6、推动系统的持续优化和迭代,确保系统能够适应不断变化的业务需求和客户期望,7、IBOS2.0版本切换的核心应用,负责核心功能的重构和优化,确保了系统平滑过渡和稳定运行',
 1),

(6, 2, '商业管理系统', '2017.07 - 2018.05',
 '为自建的商场提供一套完整的商业管理系统,通过自研和外采的方式提供一套涵盖商户入驻、合同管理、账单结算、数据分析、物业管理、停车管理、酒店管理等核心功能,支持商场的日常运营和决策需求',
 '1、作为核心开发与项目协调人,负责商家移动App和支付宝小程序应用入口的服务支持 2、与IB平台、实想等外部团队密切沟通,制定一套统一的接口对接方案 3、为其他业务团队提供接入指导,推动了平台应用内部服务化的统一标准。4、基于新的技术架构复刻办公园区的小邮局系统,分解任务、并推动开发计划落地',
 2),

(7, 2, '智慧办公系统', '2016.07 - 2017.06',
 '旨在解决办公园区的商业化运作的探索,通过数字化手段提升园区的运营效率和服务质量,系统涵盖了组织架构、用户、权限、会议室、访客、小邮局、账单等核心能力,支持多业务场景下的园区管理和协同的一套综合解决方案',
 '1、从0开始参与平台建设,负责核心服务的开发、开放平台的对接以及团队管理。2、负责组织架构、权限、会议室、访客、小邮局、账单等核心服务应用的开发与后续服务能力开放 3、为上层业务应用打下坚实基础。应用层面拥抱阿里云原生技术,服务层面利用HSF、Diamond、Tair等中间件构建了高可用的分布式服务。4、负责新伙伴的入职面试与指导,5、主导线上问题的快速响应流程与文档沉淀,保障了系统稳定运行。',
 3);

-- === 工作经历 3 的项目 ===
INSERT INTO project (id, experience_id, name, lifecycle, background, duty, sort_order) VALUES
(8, 3, '每日新闻系统', '2014.12 - 2016.06',
 '为每日新闻提供一套完整的新闻发布和管理系统,通过数字化手段提升新闻发布的效率和质量,系统涵盖了新闻编辑、审核、发布、数据分析等核心功能,支持新闻的全流程管理和优化',
 '1、负责系统的需求分析、设计和开发,确保系统能够满足客户的业务需求。2、与日本客户紧密沟通,确保需求的准确理解和及时交付。3、处理跨语言系统间的通信与文件解析(C#/Java),解决数据交互难题。4、负责系统性能优化、Bug修复及运维工具开发(C语言),积累了宝贵的多语言问题排查经验。',
 1);

-- ============================================
-- 7. 初始化数据 - 项目成果
-- ============================================

-- 项目 1: 设计空间改造
INSERT INTO project_achievement (project_id, content, sort_order) VALUES
(1, '【架构创新】设计内外部双轨架构,支持企业级私有化部署与SaaS化服务双模式', 1),
(1, '【效率提升】项目提前2个月上线,给业务留下充裕的时间,获得业务部门100%满意度评价', 2),
(1, '【技术突破】引入虚幻引擎+AI技术栈,实现3D空间实时渲染与智能设计推荐', 3),
(1, '【持续优化】基于用户反馈迭代12个版本,系统性能提升100%', 4);

-- 项目 2: 成本采购系统、供应商管理系统
INSERT INTO project_achievement (project_id, content, sort_order) VALUES
(2, '【业务规模】管理供应商超2000家,服务用户12万+,年采购金额突破400亿', 1),
(2, '【合同管理】实时在线管理合同2000+份,多端合同审批效率提升80%', 2),
(2, '【系统重构】完成核心业务重构,系统平均响应时间降低70%,稳定性达99.9%', 3),
(2, '【合规保障】建立完整审计链路,满足金融级合规要求', 4);

-- 项目 3: 设计管理系统、审批流系统
INSERT INTO project_achievement (project_id, content, sort_order) VALUES
(3, '【系统架构】一键开租模式,支持多租户多业态,部署时间从周缩短至小时', 1),
(3, '【文件管理】管理文件总量超100TB,日处理图纸文件10万+,版本控制精度达秒级', 2),
(3, '【业务覆盖】服务10+业务线,成为集团级文件基础设施,调用量月均增长50%', 3),
(3, '【审批引擎】接入多审批引擎,支撑10000+业务实例审批场景,人员变更零影响', 4);

-- 项目 4: 工程管理系统
INSERT INTO project_achievement (project_id, content, sort_order) VALUES
(4, '【客户覆盖】服务阿里、蚂蚁、浙大等头部客户,覆盖全国60+项目现场', 1),
(4, '【数据规模】管理工程项目问题100万+,构建行业最大现场管理数据库', 2),
(4, '【技术创新】攻克弱网/无网环境技术难题,实现离线模式100%可用', 3),
(4, '【模式验证】敏捷开发+MVP模式验证成功,应对业务需求变化响应速度提升5倍', 4);

-- 项目 5: 物业管理系统
INSERT INTO project_achievement (project_id, content, sort_order) VALUES
(5, '【架构升级】引入DDD领域驱动设计,业务逻辑清晰度提升,开发效率提高40%', 1),
(5, '【园区支持】成功支持多园区多租户复杂业务场景,提升客户满意度', 2),
(5, '【平滑迁移】主导IBOS2.0版本切换,零故障迁移,用户无感知升级', 3),
(5, '【团队培养】建立Code Review机制,团队业务响应速度和技术水平显著提升', 4);

-- 项目 6: 商业管理系统
INSERT INTO project_achievement (project_id, content, sort_order) VALUES
(6, '【核心能力】构建IBOS1.0平台核心服务,支撑商户入驻、停车、人脸识别等关键业务', 1),
(6, '【标准制定】制定统一接口标准,推动平台服务化,对接效率提升60%', 2),
(6, '【生态合作】与IB平台、实想等外部团队深度合作,同时支持移动App和支付宝小程序,构建完整商业生态', 3);

-- 项目 7: 智慧办公系统
INSERT INTO project_achievement (project_id, content, sort_order) VALUES
(7, '【平台奠基】从0到1构建IB平台基础能力(IBOS1.0),成为集团级基础设施', 1),
(7, '【流程优化】首创OneFace全流程权限管控,园区运营效率提升50%', 2),
(7, '【业务复刻】为雄安新区提供完整解决方案模板,实现快速复制部署', 3),
(7, '【团队建设】负责新伙伴面试指导,建立快速响应机制,保障系统99.9%可用', 4);

-- 项目 8: 每日新闻系统
INSERT INTO project_achievement (project_id, content, sort_order) VALUES
(8, '【系统稳定】保障每日新闻系统7×24小时稳定运行,零故障记录', 1),
(8, '【性能优化】开发C语言运维工具,系统性能提升30%', 2);

-- ============================================
-- 8. 初始化数据 - 技术栈
-- ============================================

-- 工作经历 1 技术栈
INSERT INTO work_experience_stack (experience_id, tech_name, sort_order) VALUES
(1, 'Java', 1),
(1, 'Spring Boot', 2),
(1, 'RPC(HSF/Dubbo)', 3),
(1, 'RocketMQ', 4),
(1, 'Redis', 5),
(1, 'MongoDB', 6),
(1, 'MySQL', 7),
(1, 'EDAS、DRDS、OSS', 8);

-- 工作经历 2 技术栈
INSERT INTO work_experience_stack (experience_id, tech_name, sort_order) VALUES
(2, 'Java', 1),
(2, 'Spring/SpringMVC', 2),
(2, 'RPC(HSF/Dubbo)', 3),
(2, 'Golang', 4),
(2, 'RocketMQ', 5),
(2, 'MySQL', 6),
(2, 'Redis', 7),
(2, 'EDAS、DRDS、OSS', 8),
(2, 'DDD', 9);

-- 工作经历 3 技术栈
INSERT INTO work_experience_stack (experience_id, tech_name, sort_order) VALUES
(3, 'Java', 1),
(3, 'C#', 2),
(3, 'C', 3),
(3, 'Oracle', 4),
(3, 'MySQL', 5);

-- ============================================
-- 9. 验证
-- ============================================
SELECT '简历数据初始化完成！' AS status;
SELECT COUNT(*) AS experience_count FROM work_experience;
SELECT COUNT(*) AS project_count FROM project;
SELECT COUNT(*) AS achievement_count FROM project_achievement;
SELECT COUNT(*) AS stack_count FROM work_experience_stack;

