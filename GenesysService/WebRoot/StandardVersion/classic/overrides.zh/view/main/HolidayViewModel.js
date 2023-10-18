Ext.define('Tab.overrides.view.main.HolidayViewModel',{
    override: 'Tab.view.main.HolidayViewModel', 
    config:{
        data:{
            workdayView: '工作日配置',
            Workdays: '工作日',
            Monday:'周1',
            Tuesday:'周2', 
            Wednesday:'周3',
            Thursday:'周4',
            Friday:'周5',
            Saturday:'周6',
            Sunday:'周日',
            Worktime:'工作时间',
            Department:'部门',
            DutyTelephone:'值班电话',
            DutyTelephoneTooltip:'在"分段"设置里,可以取消该值班电话',
			DutyTelephoneMainTooltip:'此处无值班电话,则使用默认上班时间里设置的值班电话',
            Work:'工作',
            Week:'星期',
            Starttime:'开始时间',
            StarttimeDirtyText:'开始时间 已编辑',
            Endtime:'结束时间',
            EndtimeDirtyText:'结束时间 已编辑',
            Createdate:'创建时间',
            Updatedate:'更新时间',
            Subsection:'分段',
            Refresh:'刷新',
            Close:'关闭',
            AddNew:'添 加',
            holidayName:'名称',
            DepartmentID:'部门编号',
            DateColumnName:'日期',
            DeleteName:'删除',
            DeleteMessage:'请确认删除!'
        }
    }
});
