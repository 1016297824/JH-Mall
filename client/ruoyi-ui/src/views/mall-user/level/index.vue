<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="160px">
            <el-form-item label="等级名称" prop="levelName">
        <el-input
          v-model="queryParams.levelName"
          placeholder="请输入等级名称"
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
          v-hasPermi="['mall-user:level:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mall-user:level:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mall-user:level:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mall-user:level:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="levelList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键，自增" align="center" prop="id" />
      <el-table-column label="等级名称" align="center" prop="levelName" />
      <el-table-column label="等级值" align="center" prop="levelValue" />
      <el-table-column label="该等级所需的最低成长值" align="center" prop="minGrowth" />
      <el-table-column label="该等级的最高成长值" align="center" prop="maxGrowth" />
      <el-table-column label="等级图标 URL" align="center" prop="icon" />
      <el-table-column label="权益" align="center" prop="benefitsJson" />
      <el-table-column label="逻辑删除标志" align="center" prop="isDeleted" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall-user:level:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall-user:level:remove']">删除</el-button>
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

    <!-- 添加或修改会员等级定义对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="levelRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="24">
      <el-form-item label="等级名称" prop="levelName">
              <el-input v-model="form.levelName" placeholder="请输入等级名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="等级图标 URL" prop="icon">
              <el-input v-model="form.icon" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="权益" prop="benefitsJson">
              <el-input v-model="form.benefitsJson" type="textarea" placeholder="请输入内容" />
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

<script setup lang="ts" name="Level">
import type { MallUserMemberLevel, LevelQueryParams } from "@/types/api/mall-user/level"
import { listLevel, getLevel, delLevel, addLevel, updateLevel } from "@/api/mall-user/level"

const { proxy } = getCurrentInstance()

const levelList = ref<MallUserMemberLevel[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>("")

const data = reactive({
  form: {} as MallUserMemberLevel,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    levelName: undefined,
    levelValue: undefined,
    minGrowth: undefined,
    maxGrowth: undefined,
    icon: undefined,
    benefitsJson: undefined,
    isDeleted: undefined,
  } as LevelQueryParams,
  rules: {
    levelName: [
      { required: true, message: "等级名称不能为空", trigger: "blur" }
    ],
    levelValue: [
      { required: true, message: "等级值不能为空", trigger: "blur" }
    ],
    minGrowth: [
      { required: true, message: "该等级所需的最低成长值不能为空", trigger: "blur" }
    ],
    maxGrowth: [
      { required: true, message: "该等级的最高成长值不能为空", trigger: "blur" }
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

/** 查询会员等级定义列表 */
function getList() {
  loading.value = true
  listLevel(queryParams.value).then(response => {
    levelList.value = response.rows
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
    levelName: null,
    levelValue: null,
    minGrowth: null,
    maxGrowth: null,
    icon: null,
    benefitsJson: null,
    isDeleted: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("levelRef")
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
function handleSelectionChange(selection: MallUserMemberLevel[]) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加会员等级定义"
}

/** 修改按钮操作 */
function handleUpdate(row: MallUserMemberLevel) {
  reset()
  const _id = row.id || ids.value[0]
  getLevel(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改会员等级定义"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["levelRef"].validate((valid: boolean) => {
    if (valid) {
      if (form.value.id != null) {
        updateLevel(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addLevel(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row: MallUserMemberLevel) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm('是否确认删除会员等级定义编号为"' + _ids + '"的数据项？').then(function() {
    return delLevel(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mall-user/level/export', {
    ...queryParams.value
  }, `level_${new Date().getTime()}.xlsx`)
}

getList()
</script>
