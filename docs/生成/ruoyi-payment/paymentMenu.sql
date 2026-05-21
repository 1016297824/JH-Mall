-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('支付单', '2005', '1', 'payment', 'mall-payment/payment/index', 1, 0, 'C', '0', '0', 'mall-payment:payment:list', '#', 'admin', sysdate(), '', null, '支付单菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('支付单查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mall-payment:payment:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('支付单新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mall-payment:payment:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('支付单修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mall-payment:payment:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('支付单删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mall-payment:payment:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('支付单导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mall-payment:payment:export',       '#', 'admin', sysdate(), '', null, '');