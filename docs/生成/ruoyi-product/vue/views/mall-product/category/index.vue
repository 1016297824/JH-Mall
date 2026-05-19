<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="类目名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入类目名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="类目路径，如 /1/10/100" prop="path">
        <el-input
          v-model="queryParams.path"
          placeholder="请输入类目路径，如 /1/10/100"
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
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-product:category:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-product:category:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-product:category:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="categoryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键，自增" align="center" prop="id" />
      <el-table-column label="父类目 ID，0 表示顶级类目" align="center" prop="parentId" />
      <el-table-column label="类目名称" align="center" prop="name" />
      <el-table-column label="类目层级" align="center" prop="level" />
      <el-table-column label="类目标识图标 URL" align="center" prop="icon" />
      <el-table-column label="排序值，越小越靠前" align="center" prop="sortOrder" />
      <el-table-column label="是否前端可见" align="center" prop="isVisible" />
      <el-table-column label="类目路径，如 /1/10/100" align="center" prop="path" />
      <el-table-column label="逻辑删除标志" align="center" prop="isDeleted" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-product:category:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-product:category:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改商品类目对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="categoryRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="类目名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入类目名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="类目标识图标 URL" prop="icon">
              <el-input v-model="form.icon" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="类目路径，如 /1/10/100" prop="path">
              <el-input v-model="form.path" placeholder="请输入类目路径，如 /1/10/100" />
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
import type { MallProductCategory, CategoryQueryParams } from "@/types/api/mall-product/category"
import { listCategory, getCategory, delCategory, addCategory, updateCategory } from "@/api/mall-product/category"

const { proxy } = getCurrentInstance()

const categoryList = ref<MallProductCategory[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallProductCategory,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    parentId: undefined,
    name: undefined,
    level: undefined,
    icon: undefined,
    sortOrder: undefined,
    isVisible: undefined,
    path: undefined,
    isDeleted: undefined,
  } as CategoryQueryParams,
  rules: {
    parentId: [
      { required: true, message: "父类目 ID，0 表示顶级类目不能为空", trigger: "blur" }
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
    categoryList.value = response.rows
    total.value = response.total
    loading.value = false
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
    isDeleted: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("categoryRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection: MallProductCategory[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加商品类目"
}

/** 修改按钮操作 */
function handleUpdate(row: MallProductCategory) {
  reset()
  const _id = row.id || ids.value[0]
  getCategory(_id).then(response => {
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
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除商品类目编号为"' + _ids + '"的数据项？').then(function() {
    return delCategory(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-product/category/export', {
    ...queryParams.value
  }, `category_${new Date().getTime()}.xlsx`)
}

getList()
</script>
