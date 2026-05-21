-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('支付渠道', '2005', '1', 'channel', 'mall-payment/channel/index', 1, 0, 'C', '0', '0', 'mall-payment:channel:list', '#', 'admin', sysdate(), '', null, '支付渠道菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('支付渠道查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mall-payment:channel:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('支付渠道新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mall-payment:channel:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('支付渠道修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mall-payment:channel:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('支付渠道删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mall-payment:channel:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('支付渠道导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mall-payment:channel:export',       '#', 'admin', sysdate(), '', null, '');