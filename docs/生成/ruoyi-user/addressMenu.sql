-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('地址簿', '2002', '1', 'address', 'mall-user/address/index', 1, 0, 'C', '0', '0', 'mall-user:address:list', '#', 'admin', sysdate(), '', null, '地址簿菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('地址簿查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:address:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('地址簿新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:address:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('地址簿修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:address:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('地址簿删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:address:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('地址簿导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:address:export',       '#', 'admin', sysdate(), '', null, '');