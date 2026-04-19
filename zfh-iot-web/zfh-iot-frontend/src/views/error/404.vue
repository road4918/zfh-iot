<template>
  <div class="error-page">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  
  h1 {
    font-size: 100px;
    color: #409eff;
    margin-bottom: 20px;
  }
  
  p {
    font-size: 20px;
    color: #606266;
    margin-bottom: 30px;
  }
}
</style>
