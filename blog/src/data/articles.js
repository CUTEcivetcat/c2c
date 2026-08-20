// 博客文章数据（纯静态，构建时打包进前端）
// 新增文章：在 src/data/posts/ 下新建一个 markdown 文件，然后在下面加一条记录

import p1 from './posts/1.md?raw'
import p2 from './posts/2.md?raw'
import p3 from './posts/3.md?raw'
import p4 from './posts/4.md?raw'

export const categories = ['技术', '生活', '随笔']

export const articles = [
  {
    id: 1,
    title: '你好，博客',
    category: '随笔',
    date: '2026-08-04',
    summary: '第一篇博客，介绍一下这个小站和它背后的项目。',
    content: p1,
  },
  {
    id: 2,
    title: 'C2C 二手交易平台部署记录',
    category: '技术',
    date: '2026-08-03',
    summary: 'Spring Boot 单体 + 双前端 + Nginx 的完整部署过程记录。',
    content: p2,
  },
  {
    id: 3,
    title: '读书笔记：把时间当作朋友',
    category: '生活',
    date: '2026-07-28',
    summary: '时间不可管理，能管理的只有我们自己。',
    content: p3,
  },
  {
    id: 4,
    title: '前端性能优化的一点笔记',
    category: '技术',
    date: '2026-07-20',
    summary: '图片懒加载、路由懒加载、构建体积……几个立竿见影的小优化。',
    content: p4,
  },
]
