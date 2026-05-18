-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('积分账户', '2002', '1', 'account', 'mall-user/account/index', 1, 0, 'C', '0', '0', 'mall-user:account:list', '#', 'admin', sysdate(), '', null, '积分账户菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('积分账户查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:account:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('积分账户新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:account:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('积分账户修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:account:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('积分账户删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:account:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('积分账户导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:account:export',       '#', 'admin', sysdate(), '', null, '');