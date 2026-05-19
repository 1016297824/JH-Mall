-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('订单管理', '2004', '1', 'order', 'mall-order/order/index', 1, 0, 'C', '0', '0', 'mall-order:order:list', '#', 'admin', sysdate(), '', null, '订单管理菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('订单管理查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:order:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('订单管理新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:order:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('订单管理修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:order:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('订单管理删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:order:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('订单管理导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:order:export',       '#', 'admin', sysdate(), '', null, '');