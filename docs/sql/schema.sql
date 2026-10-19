-- ============================================================
-- 邻里鲜生社区超市商城系统 · 建表脚本（schema.sql）
-- 数据库：MySQL 8.0.16+
-- 说明：本文件由 docs/数据库设计文档.md 生成，共 20 张表
-- 执行：mysql -uapp_rw -p linli_fresh < schema.sql
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE t_user (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  phone         VARCHAR(20)     NOT NULL COMMENT '手机号（登录账号）',
  password      VARCHAR(100)    NOT NULL COMMENT 'BCrypt 密码哈希',
  nickname      VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '昵称',
  user_type     TINYINT         NOT NULL COMMENT '1会员 2运营 3管理员',
  role_id       BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  status        TINYINT         NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_phone (phone),
  KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表（前后台合用）';

CREATE TABLE t_user_address (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  receiver      VARCHAR(50)     NOT NULL COMMENT '收货人',
  phone         VARCHAR(20)     NOT NULL COMMENT '联系电话',
  detail        VARCHAR(255)    NOT NULL COMMENT '详细地址',
  is_default    TINYINT         NOT NULL DEFAULT 0 COMMENT '1默认 0非默认',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  KEY idx_user_default (user_id, is_default, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收货地址表';

CREATE TABLE t_role (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  name          VARCHAR(50)     NOT NULL COMMENT '角色名',
  code          VARCHAR(50)     NOT NULL COMMENT '角色编码',
  remark        VARCHAR(255)    NULL COMMENT '备注',
  status        TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

CREATE TABLE t_permission (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  code          VARCHAR(100)    NOT NULL COMMENT '权限点编码（模块:资源:操作）',
  name          VARCHAR(100)    NOT NULL COMMENT '权限点名称',
  module        VARCHAR(50)     NOT NULL COMMENT '所属模块',
  type          TINYINT         NOT NULL DEFAULT 3 COMMENT '1菜单 2按钮 3接口',
  parent_id     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父权限点，0为根',
  sort          INT             NOT NULL DEFAULT 0 COMMENT '排序',
  status        TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code),
  KEY idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限点表';

CREATE TABLE t_role_permission (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_id       BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  permission_id BIGINT UNSIGNED NOT NULL COMMENT '权限点ID',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_permission (role_id, permission_id),
  KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';

CREATE TABLE t_category (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  name          VARCHAR(50)     NOT NULL COMMENT '分类名',
  sort          INT             NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  status        TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  KEY idx_name (name),
  KEY idx_status_sort (status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类表';

CREATE TABLE t_product (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  category_id   BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
  name          VARCHAR(128)    NOT NULL COMMENT '商品名称',
  spec          VARCHAR(128)    NULL COMMENT '规格',
  image         VARCHAR(255)    NULL COMMENT '主图URL',
  description   TEXT            NULL COMMENT '商品描述',
  price         BIGINT UNSIGNED NOT NULL COMMENT '售价（分）',
  status        TINYINT         NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  KEY idx_category_status_deleted (category_id, status, deleted),
  KEY idx_status_update (status, update_time),
  KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品表（单层商品）';

CREATE TABLE t_inventory (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  product_id    BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  total         INT             NOT NULL DEFAULT 0 COMMENT '总库存',
  locked        INT             NOT NULL DEFAULT 0 COMMENT '锁定库存',
  sold          INT             NOT NULL DEFAULT 0 COMMENT '已售库存',
  version       INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_product_id (product_id),
  CONSTRAINT chk_inventory_nonneg CHECK (total >= 0 AND locked >= 0 AND sold >= 0 AND locked + sold <= total)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存表（三态库存）';

CREATE TABLE t_cart_item (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  product_id    BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  quantity      INT             NOT NULL DEFAULT 1 COMMENT '数量（1~99）',
  checked       TINYINT         NOT NULL DEFAULT 1 COMMENT '1勾选 0未勾选',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_product (user_id, product_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车项表（物理删除）';

CREATE TABLE t_order (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_no        VARCHAR(32)     NOT NULL COMMENT '订单号',
  user_id         BIGINT UNSIGNED NOT NULL COMMENT '会员ID',
  address_id      BIGINT UNSIGNED NULL COMMENT '收货地址ID（自提为空）',
  fulfill_type    TINYINT         NOT NULL COMMENT '1配送 2自提',
  total_amount    BIGINT UNSIGNED NOT NULL COMMENT '商品总额（分）',
  discount_amount BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '优惠金额（分）',
  pay_amount      BIGINT UNSIGNED NOT NULL COMMENT '实付金额（分）',
  coupon_id       BIGINT UNSIGNED NULL COMMENT '用户优惠券ID',
  status          TINYINT         NOT NULL DEFAULT 0 COMMENT '0待支付 1待发货 2待收货 3已完成 4已取消 5售后中 6已退款',
  pay_deadline    DATETIME        NULL COMMENT '支付截止时间',
  remark          VARCHAR(255)    NULL COMMENT '备注',
  create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_user_status (user_id, status, deleted),
  KEY idx_status_deadline (status, pay_deadline),
  KEY idx_create_time (create_time),
  CONSTRAINT chk_order_amount CHECK (total_amount >= 0 AND discount_amount >= 0 AND pay_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';

CREATE TABLE t_order_item (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id      BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  product_id    BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  product_name  VARCHAR(128)    NOT NULL COMMENT '商品名快照',
  spec          VARCHAR(128)    NULL COMMENT '规格快照',
  price         BIGINT UNSIGNED NOT NULL COMMENT '成交单价（分，快照）',
  quantity      INT             NOT NULL COMMENT '数量',
  amount        BIGINT UNSIGNED NOT NULL COMMENT '小计（分）',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_order_id (order_id),
  KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单项表（商品快照）';

CREATE TABLE t_payment (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  pay_no        VARCHAR(32)     NOT NULL COMMENT '支付单号（幂等键）',
  order_id      BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  user_id       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  amount        BIGINT UNSIGNED NOT NULL COMMENT '支付金额（分）',
  channel       TINYINT         NOT NULL DEFAULT 1 COMMENT '支付渠道：1沙箱',
  status        TINYINT         NOT NULL DEFAULT 0 COMMENT '0待支付 1支付中 2成功 3失败 4已关闭',
  pay_time      DATETIME        NULL COMMENT '支付成功时间',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_pay_no (pay_no),
  KEY idx_order_id (order_id),
  KEY idx_user_id (user_id),
  KEY idx_status_create (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付单表';

CREATE TABLE t_refund (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  refund_no     VARCHAR(32)     NOT NULL COMMENT '退款单号（幂等键）',
  payment_id    BIGINT UNSIGNED NOT NULL COMMENT '支付单ID',
  order_id      BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  after_sale_id BIGINT UNSIGNED NULL COMMENT '关联售后单ID',
  amount        BIGINT UNSIGNED NOT NULL COMMENT '退款金额（分）',
  status        TINYINT         NOT NULL DEFAULT 0 COMMENT '0退款中 1成功 2失败',
  refund_time   DATETIME        NULL COMMENT '退款成功时间',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_refund_no (refund_no),
  KEY idx_payment_id (payment_id),
  KEY idx_order_id (order_id),
  KEY idx_after_sale_id (after_sale_id),
  KEY idx_status_create (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款单表';

CREATE TABLE t_after_sale (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  sale_no       VARCHAR(32)     NOT NULL COMMENT '售后单号',
  order_id      BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  user_id       BIGINT UNSIGNED NOT NULL COMMENT '会员ID',
  type          TINYINT         NOT NULL COMMENT '1退款 2退货退款',
  reason        VARCHAR(255)    NOT NULL COMMENT '申请原因',
  amount        BIGINT UNSIGNED NOT NULL COMMENT '申请退款金额（分）',
  status        TINYINT         NOT NULL DEFAULT 0 COMMENT '0待审核 1已同意 2已驳回 3退款中 4已完成',
  pre_status    TINYINT         NOT NULL COMMENT '申请前订单状态',
  refund_no     VARCHAR(32)     NULL COMMENT '关联退款单号',
  audit_time    DATETIME        NULL COMMENT '审核时间',
  remark        VARCHAR(255)    NULL COMMENT '处理备注',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sale_no (sale_no),
  KEY idx_order_id (order_id),
  KEY idx_user_status (user_id, status),
  KEY idx_status_update (status, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='售后单表';

CREATE TABLE t_coupon (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  name          VARCHAR(100)    NOT NULL COMMENT '券名称',
  type          TINYINT         NOT NULL DEFAULT 1 COMMENT '1满减',
  threshold     BIGINT UNSIGNED NOT NULL COMMENT '使用门槛（分）',
  discount      BIGINT UNSIGNED NOT NULL COMMENT '优惠面额（分）',
  total         INT             NOT NULL COMMENT '发放总量',
  received      INT             NOT NULL DEFAULT 0 COMMENT '已领取数量',
  start_time    DATETIME        NOT NULL COMMENT '生效时间',
  end_time      DATETIME        NOT NULL COMMENT '失效时间',
  status        TINYINT         NOT NULL DEFAULT 1 COMMENT '1可领 0停用',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  KEY idx_status_time (status, start_time, end_time),
  KEY idx_name (name),
  CONSTRAINT chk_coupon_amount CHECK (discount <= threshold AND total >= 0 AND received >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券表';

CREATE TABLE t_user_coupon (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  coupon_id     BIGINT UNSIGNED NOT NULL COMMENT '优惠券ID',
  status        TINYINT         NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用 2已过期',
  used_order_id BIGINT UNSIGNED NULL COMMENT '使用的订单ID',
  receive_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  use_time      DATETIME        NULL COMMENT '使用时间',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_coupon (user_id, coupon_id),
  KEY idx_user_status (user_id, status),
  KEY idx_used_order (used_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户优惠券表';

CREATE TABLE t_review (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id      BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  order_item_id BIGINT UNSIGNED NOT NULL COMMENT '订单项ID',
  user_id       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  product_id    BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  score         TINYINT         NOT NULL COMMENT '评分1~5',
  content       VARCHAR(1000)   NULL COMMENT '评价内容',
  images        VARCHAR(1000)   NULL COMMENT '图片URL数组（JSON字符串）',
  reply         VARCHAR(1000)   NULL COMMENT '运营回复',
  reply_time    DATETIME        NULL COMMENT '回复时间',
  status        TINYINT         NOT NULL DEFAULT 1 COMMENT '1正常 0隐藏',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_item (order_item_id),
  KEY idx_product_status_time (product_id, status, create_time),
  KEY idx_user_id (user_id),
  CONSTRAINT chk_review_score CHECK (score BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价表';

CREATE TABLE t_system_config (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  config_key    VARCHAR(100)    NOT NULL COMMENT '配置键',
  config_value  VARCHAR(1000)   NOT NULL COMMENT '配置值',
  description   VARCHAR(255)    NULL COMMENT '说明',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';

CREATE TABLE t_operation_log (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT UNSIGNED NULL COMMENT '操作人ID',
  module        VARCHAR(50)     NOT NULL COMMENT '模块',
  action        VARCHAR(50)     NOT NULL COMMENT '动作',
  target        VARCHAR(100)    NULL COMMENT '目标对象',
  trace_id      VARCHAR(64)     NULL COMMENT '链路ID',
  ip            VARCHAR(64)     NULL COMMENT '客户端IP',
  detail        TEXT            NULL COMMENT '变更详情（JSON）',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_user_time (user_id, create_time),
  KEY idx_trace_id (trace_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';

CREATE TABLE t_idempotent (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  idem_key      VARCHAR(64)     NOT NULL COMMENT '客户端幂等键',
  user_id       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  biz_type      VARCHAR(32)     NOT NULL COMMENT '业务类型',
  biz_no        VARCHAR(32)     NULL COMMENT '生成的业务单号',
  status        TINYINT         NOT NULL DEFAULT 0 COMMENT '0处理中 1成功 2失败',
  create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  expire_time   DATETIME        NOT NULL COMMENT '过期时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_biz_key (user_id, biz_type, idem_key),
  KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='幂等记录表';
SET FOREIGN_KEY_CHECKS = 1;
