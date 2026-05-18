-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('成长值流水', '2002', '1', 'growth_log', 'mall-user/growth_log/index', 1, 0, 'C', '0', '0', 'mall-user:growth_log:list', '#', 'admin', sysdate(), '', null, '成长值流水菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('成长值流水查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:growth_log:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('成长值流水新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:growth_log:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('成长值流水修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:growth_log:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('成长值流水删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:growth_log:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('成长值流水导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:growth_log:export',       '#', 'admin', sysdate(), '', null, '');