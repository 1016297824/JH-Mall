-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('优惠券定义', '2006', '1', 'coupon', 'mall-marketing/coupon/index', 1, 0, 'C', '0', '0', 'mall-marketing:coupon:list', '#', 'admin', sysdate(), '', null, '优惠券定义菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('优惠券定义查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mall-marketing:coupon:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('优惠券定义新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mall-marketing:coupon:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('优惠券定义修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mall-marketing:coupon:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('优惠券定义删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mall-marketing:coupon:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('优惠券定义导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mall-marketing:coupon:export',       '#', 'admin', sysdate(), '', null, '');