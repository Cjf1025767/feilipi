Ext.define('Tab.overrides.view.main.HolidayViewModel',{
    override: 'Tab.view.main.HolidayViewModel', 
    config:{
        data:{
            workdayView: 'Workday view',
            Workdays: 'Workdays',
            Monday:'Monday',
            Tuesday:'Tuesday', 
            Wednesday:'Wednesday',
            Thursday:'Thursday',
            Friday:'Friday',
            Saturday:'Saturday',
            Sunday:'Sunday',
            Worktime:'Work time',
            Department:'Department',
            DutyTelephone:'Duty telephone',
            DutyTelephoneTooltip:'In the subsection setting, you can cancel the duty phone',
            DutyTelephoneMainTooltip:'If there is no duty telephone here, the duty telephone set in the default working hours will be used',
            Work:'Work',
            Week:'Week',
            Starttime:'Starttime',
            StarttimeDirtyText:'Starttime edited',
            Endtime:'Endtime',
            EndtimeDirtyText:'Endtime edited',
            Createdate:'Createdate',
            Updatedate:'Updatedate',
            Subsection:'Subsection',
            Refresh:'Refresh',
            Close:'Close',
            AddNew:'AddNew',
            holidayName:'Name',
            DepartmentID:'DepartmentID',
            DateColumnName:'Date',
            DeleteName:'Delete',
            DeleteMessage:'Please confirm the deletion'
        }
    }
});
