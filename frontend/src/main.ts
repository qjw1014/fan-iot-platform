import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  Alert,
  Badge,
  Breadcrumb,
  Button,
  Card,
  Checkbox,
  Col,
  ConfigProvider,
  DatePicker,
  Descriptions,
  Divider,
  Drawer,
  Dropdown,
  Empty,
  Form,
  Input,
  InputNumber,
  Layout,
  List,
  Menu,
  Modal,
  Pagination,
  Popconfirm,
  Radio,
  Row,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Tooltip
} from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'

import App from './App.vue'
import router from './router'
import './styles/index.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Alert)
app.use(Badge)
app.use(Breadcrumb)
app.use(Button)
app.use(Card)
app.use(Checkbox)
app.use(Col)
app.use(ConfigProvider)
app.use(DatePicker)
app.use(Descriptions)
app.use(Divider)
app.use(Drawer)
app.use(Dropdown)
app.use(Empty)
app.use(Form)
app.use(Input)
app.use(InputNumber)
app.use(Layout)
app.use(List)
app.use(Menu)
app.use(Modal)
app.use(Pagination)
app.use(Popconfirm)
app.use(Radio)
app.use(Row)
app.use(Select)
app.use(Space)
app.use(Statistic)
app.use(Switch)
app.use(Table)
app.use(Tag)
app.use(Tooltip)
app.mount('#app')
