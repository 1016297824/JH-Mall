-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('购物车', '2004', '1', 'cart', 'mall-order/cart/index', 1, 0, 'C', '0', '0', 'mall-order:cart:list', '#', 'admin', sysdate(), '', null, '购物车菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('购物车查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:cart:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('购物车新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:cart:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('购物车修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:cart:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('购物车删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:cart:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('购物车导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mall-order:cart:export',       '#', 'admin', sysdate(), '', null, '');