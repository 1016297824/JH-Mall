-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('金额快照', '2004', '1', 'amount', 'mall-order/amount/index', 1, 0, 'C', '0', '0', 'mall-order:amount:list', '#', 'admin', sysdate(), '', null, '金额快照菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('金额快照查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:amount:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('金额快照新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:amount:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('金额快照修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:amount:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('金额快照删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:amount:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('金额快照导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:amount:export',       '#', 'admin', sysdate(), '', null, '');