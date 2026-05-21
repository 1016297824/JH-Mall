<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="160px">
      <el-form-item label="类目名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入类目名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="类目路径" prop="path">
        <el-input
          v-model="queryParams.path"
          placeholder="请输入类目路径"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['mall-product:category:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="Sort"
          @click="toggleExpandAll"
        >展开/折叠</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      v-if="refreshTable"
      v-loading="loading"
      :data="categoryList"
      row-key="id"
      :default-expand-all="isExpandAll"
      :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
    >
      <el-table-column label="父类目" prop="parentId" />
      <el-table-column label="类目名称" align="center" prop="name" />
      <el-table-column label="类目层级" align="center" prop="level" />
      <el-table-column label="类目标识图标" align="center" prop="icon" />
      <el-table-column label="排序值" align="center" prop="sortOrder" />
      <el-table-column label="是否前端可见" align="center" prop="isVisible" />
      <el-table-column label="类目路径" align="center" prop="path" />

      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-product:category:edit']">修改</el-button>
          <el-button link type="primary" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['mall-product:category:add']">新增</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-product:category:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改商品类目对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="categoryRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="父类目" prop="parentId">
              <el-tree-select
                v-model="form.parentId"
                :data="categoryOptions"
                :props="{ value: 'id', label: 'name', children: 'children' }"
                value-key="id"
                placeholder="请选择父类目"
                check-strictly
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="类目名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入类目名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="类目标识图标" prop="icon">
              <el-input v-model="form.icon" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="类目路径" prop="path">
              <el-input v-model="form.path" placeholder="请输入类目路径" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Category">
import { listCategory, getCategory, delCategory, addCategory, updateCategory } from "@/api/mall-product/category"
import type { MallProductCategory, CategoryQueryParams } from "@/types/api/mall-product/category"
import type { TreeSelect } from '@/types/api/common'

const { proxy } = getCurrentInstance()

const categoryList = ref<any[]>([])
const categoryOptions = ref<TreeSelect[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const title = ref<string>("")
const isExpandAll = ref<boolean>(true)
const refreshTable = ref<boolean>(true)

const data = reactive({
  form: {} as MallProductCategory,
  queryParams: {
    parentId: undefined,
    name: undefined,
    level: undefined,
    icon: undefined,
    sortOrder: undefined,
    isVisible: undefined,
    path: undefined,

  } as CategoryQueryParams,
  rules: {
    parentId: [
      { required: true, message: "父类目不能为空", trigger: "blur" }
    ],
    name: [
      { required: true, message: "类目名称不能为空", trigger: "blur" }
    ],
    level: [
      { required: true, message: "类目层级不能为空", trigger: "blur" }
    ],
    createTime: [
      { required: true, message: "创建时间不能为空", trigger: "blur" }
    ],
    updateTime: [
      { required: true, message: "修改时间不能为空", trigger: "blur" }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询商品类目列表 */
function getList() {
  loading.value = true
  listCategory(queryParams.value).then(response => {
    categoryList.value = proxy.handleTree(response.data, "id", "parentId")
    loading.value = false
  })
}

/** 查询商品类目下拉树结构 */
function getTreeselect() {
  listCategory().then(response => {
    categoryOptions.value = []
    const data = { id: 0, name: '顶级节点', children: [] }
    data.children = proxy.handleTree(response.data, "id", "parentId")
    categoryOptions.value.push(data)
  })
}
	
/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: null,
    parentId: null,
    name: null,
    level: null,
    icon: null,
    sortOrder: null,
    isVisible: null,
    path: null,

    createTime: null,
    updateTime: null
  }
  proxy.resetForm("categoryRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 新增按钮操作 */
function handleAdd(row: MallProductCategory) {
  reset()
  getTreeselect()
  if (row != null && row.id) {
    form.value.parentId = row.id
  } else {
    form.value.parentId = 0
  }
  open.value = true
  title.value = "添加商品类目"
}

/** 展开/折叠操作 */
function toggleExpandAll() {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  nextTick(() => {
    refreshTable.value = true
  })
}

/** 修改按钮操作 */
async function handleUpdate(row: MallProductCategory) {
  reset()
  await getTreeselect()
  if (row != null) {
    form.value.parentId = row.parentId
  }
  getCategory(row.id!).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改商品类目"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["categoryRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateCategory(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addCategory(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallProductCategory) {
  proxy.$modal.confirm('是否确认删除商品类目编号为"' + row.id + '"的数据项？').then(function() {
    return delCategory(row.id!)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

getList()
</script>

<style scoped lang="scss">
</style>

