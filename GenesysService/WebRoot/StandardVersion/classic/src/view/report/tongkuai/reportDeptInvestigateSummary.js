Ext.define("Tab.view.report.reportDeptInvestigateSummaryView", {
  extend: "Ext.grid.Panel",
  xtype: "reportDeptInvestigateSummaryView",
  //通快 部门满意度汇总
  requires: [
    "Tab.view.report.reportDeptInvestigateSummaryModel",
    "Tab.view.report.reportDeptInvestigateSummaryStore",
    "Ext.form.field.Spinner",
    "Ext.form.RadioGroup",
    "Ext.form.field.Radio",
  ],

  store: Ext.create("Tab.view.report.reportDeptInvestigateSummaryStore"),
  tbar: {
    autoScroll: true,
    items: [
      {
        xtype: "container",
        layout: "vbox",
        cls: "fix-search-btn",
        items: [
          {
            xtype: "label",
            style: {
              margin: "7px",
              fontSize: "16px",
              marginBottom: "10px",
            },
            text: "Satisfaction Summary Dept",
          },
          {
            xtype: "button",
            width: 100,
            margin: "0 0 10 8",
            text: "Query",
            listeners: {
              click: function () {
                var grid = this.up().up().up(),
                  me = this.up().up();
                if (
                  grid.down('datetimetowfield[name="starttime"]').getValue() ===
                    null ||
                  grid.down('datetimetowfield[name="starttime"]').getValue() ===
                    ""
                ) {
                  grid.down('datetimetowfield[name="starttime"]').focus();
                }
                if (
                  grid.down('datetimetowfield[name="endtime"]').getValue() ===
                    null ||
                  grid.down('datetimetowfield[name="endtime"]').getValue() ===
                    ""
                ) {
                  grid.down('datetimetowfield[name="endtime"]').focus();
                }
                grid.store.proxy.extraParams = {
                  starttime:
                    me
                      .down('datetimetowfield[name="starttime"]')
                      .getSubmitValue() + ":00",
                  endtime:
                    me
                      .down('datetimetowfield[name="endtime"]')
                      .getSubmitValue() + ":59",
                  type: me.down("radiogroup").getChecked()[0].inputValue,
                  department: grid
                    .down('combobox[fieldLabel="Department"]')
                    .getValue(),
                  queues: me
                    .down('tagfield[fieldLabel="MoreQueues"]')
                    .getValue()
                    .toString(),
                  firstdigits: me
                    .down('tagfield[fieldLabel="FirstDigits"]')
                    .getValue()
                    .toString(),
                  seconddigits: me
                    .down('tagfield[fieldLabel="SecondDigits"]')
                    .getValue()
                    .toString(),
                };
                grid.store.load();
              },
            },
          },
          {
            xtype: "button",
            width: 100,
            margin: "0 0 10 8",
            text: "Export",
            handler: function () {
              var grid = this.up().up().up(),
                me = this.up().up();
              if (
                grid.down('datetimetowfield[name="starttime"]').getValue() ===
                  null ||
                grid.down('datetimetowfield[name="starttime"]').getValue() ===
                  ""
              ) {
                grid.down('datetimetowfield[name="starttime"]').focus();
              }
              if (
                grid.down('datetimetowfield[name="endtime"]').getValue() ===
                  null ||
                grid.down('datetimetowfield[name="endtime"]').getValue() === ""
              ) {
                grid.down('datetimetowfield[name="endtime"]').focus();
              }
              grid.store.proxy.extraParams = {
                starttime:
                  me
                    .down('datetimetowfield[name="starttime"]')
                    .getSubmitValue() + ":00",
                endtime:
                  me.down('datetimetowfield[name="endtime"]').getSubmitValue() +
                  ":59",
                type: me.down("radiogroup").getChecked()[0].inputValue,
              };
              var form = Ext.create("Ext.form.Panel");
              var path = window.location.pathname;
              var pos = window.location.pathname.lastIndexOf("/");
              if (pos > 0) {
                path = window.location.pathname.substring(0, pos) + "/";
              }
              form.submit({
                target: "_blank",
                standardSubmit: true,
                url: "/tab/call/reportDeptInvestigateSummaryTKByQueues",
                method: "POST",
                hidden: true,
                headers: {
                  "Content-Type": "application/x-www-form-urlencoded",
                },
                params: {
                  script: path + "reportDeptInvestigateSummaryExport.js",
                  starttime:
                    me
                      .down('datetimetowfield[name="starttime"]')
                      .getSubmitValue() + ":00",
                  endtime:
                    me
                      .down('datetimetowfield[name="endtime"]')
                      .getSubmitValue() + ":59",
                  type: me.down("radiogroup").getChecked()[0].inputValue,
                  department: grid
                    .down('combobox[fieldLabel="Department"]')
                    .getValue(),
                  queues: me
                    .down('tagfield[fieldLabel="MoreQueues"]')
                    .getValue()
                    .toString(),
                  firstdigits: me
                    .down('tagfield[fieldLabel="FirstDigits"]')
                    .getValue()
                    .toString(),
                  seconddigits: me
                    .down('tagfield[fieldLabel="SecondDigits"]')
                    .getValue()
                    .toString(),
                },
              });
              Ext.defer(function () {
                form.close(); //延迟关闭表单(不会影响浏览器下载)
              }, 100);
            },
          },
        ],
      },
      {
        xtype: "container",
        layout: "vbox",
        items: [
          {
            xtype: "datetimetowfield",
            width: 280,
            fieldLabel: "StartTime",
            name: "starttime",
            labelWidth: 60,
            timeCfg: {
              value: "00:00",
            },
          },
          {
            xtype: "datetimetowfield",
            width: 280,
            fieldLabel: "EndTime",
            name: "endtime",
            labelWidth: 60,
            timeCfg: {
              value: "23:59",
            },
          },
        ],
      },
      {
        xtype: "container",
        layout: "vbox",
        items: [
          {
            fieldLabel: "FirstDigits",
            triggerAction: "all",
            xtype: "tagfield",
            width: 600,
            emptyText:'please choose',
            store: Ext.create("Tab.model.firstdigitStore"),
            valueField: "value",
            displayField: "name",
          },
          {
            fieldLabel: "SecondDigits",
            triggerAction: "all",
            xtype: "tagfield",
            width: 600,
            emptyText:'please choose',
            store: Ext.create("Tab.model.seconddigitStore"),
            valueField: "value",
            displayField: "name",
          },
        ],
      },
      {
        xtype: "container",
        layout: "vbox",
        items: [
          {
            xtype: "combobox",
            width: 180,
            fieldLabel: "Department",
            name: "department",
            emptyText: "All",
            labelWidth: 80,
            forceSelection: true,
            editable: true,
            queryDelay: 60000,
            triggerAction: "all",
            valueField: "roleguid",
            displayField: "rolename",
            store: Ext.create("Tab.model.groupsMenuStore"),
            listeners: {
              select: function (me, record) {
                var roleguid = record.get("roleguid");
                var comboqueues = me
                  .up()
                  .up()
                  .down('tagfield[name="morequeues"]');
                comboqueues.setValue("");
                comboqueues.store.proxy.extraParams = {
                  roleguid: roleguid,
                };
                comboqueues.store.reload();
              },
            },
          },
          {
            xtype: "radiogroup",
            width: 280,
            layout: {
              align: "middle",
              type: "hbox",
            },
            items: [
              {
                xtype: "radiofield",
                name: "investigateSummaryType",
                inputValue: "3",
                boxLabel: "Month",
                margin: "0 0 0 10",
              },
              {
                xtype: "radiofield",
                name: "investigateSummaryType",
                inputValue: "2",
                boxLabel: "Week",
                margin: "0 0 0 10",
              },
              {
                xtype: "radiofield",
                name: "investigateSummaryType",
                inputValue: "1",
                checked: true,
                boxLabel: "Day",
                margin: "0 0 0 10",
              },
              {
                xtype: "radiofield",
                name: "investigateSummaryType",
                inputValue: "0",
                boxLabel: "Hour",
                margin: "0 0 0 10",
              },
            ],
          },
        ],
      },
      {
        xtype: "container",
        layout: "vbox",
        items: [
          {
            xtype: "container",
            layout: "vbox",
            items: [
              {
                fieldLabel: "MoreQueues",
                triggerAction: "all",
                xtype: "tagfield",
                name: "morequeues",
                width: 700,
                emptyText:'please choose',
                store: Ext.create("Tab.model.queuesStore"),
                valueField: "rolename",
                displayField: "rolename",
              },
            ],
          },
        ],
      },
    ],
  },
  columns: [
    {
      xtype: "rownumberer",
      width: 38,
      align: "center",
    },
    {
      header: "UserName",
      hidden: true,
      width: 115,
      dataIndex: "username",
    },
    {
      header: "Department",
      width: 125,
      dataIndex: "department",
    },
    {
      header: "Time",
      width: 260,
      dataIndex: "nyear",
      renderer: function (
        v,
        metaData,
        record,
        rowIndex,
        colIndex,
        store,
        view
      ) {
        switch (record.get("ntype")) {
          case 0:
            return (
              record.get("nyear") +
              "-" +
              record.get("nmonth") +
              "-" +
              record.get("nday") +
              "-" +
              record.get("nhour") +
              "Hours"
            );
          case 1:
            return (
              record.get("nyear") +
              "-" +
              record.get("nmonth") +
              "-" +
              record.get("nday")
            );
          case 2:
            return record.get("nyear") + "-" + record.get("nweek") + "Weeks";
          case 3:
            return record.get("nyear") + "-" + record.get("nmonth") ;
        }
      },
    },
    {
      header: "Type",
      hidden: true,
      width: 115,
      dataIndex: "businesstype",
    },
    {
      header: "Very Satisfied",
      width: 110,
      dataIndex: "n1count",
    },
    {
      header: "Satisfied",
      width: 90,
      dataIndex: "n2count",
    },
    {
      header: "Dissatisfied",
      width: 110,
      dataIndex: "n3count",
    },
    {
      header: "Complaints&Suggestions",
      hidden: true,
      width: 90,
      dataIndex: "n4count",
    },
    {
      header: "5 points",
      hidden: true,
      width: 90,
      dataIndex: "n5count",
    },
    {
      header: "Valid count",
      flex: 1,
      dataIndex: "ntotalcallcount",
      renderer: function (
        v,
        metaData,
        record,
        rowIndex,
        colIndex,
        store,
        view
      ) {
        return v - record.get("n0count");
      },
    },
    {
      //总分
      header: "Total score",
      width: 100,
      dataIndex: "totalscore",
    },
    {
      //有效平均分
      header: "average(Valid)",
      flex: 1,
      dataIndex: "ntotalcallcount",
      renderer: function (
        v,
        metaData,
        record,
        rowIndex,
        colIndex,
        store,
        view
      ) {
        var varysctotalscoreore = parseInt(record.get("totalscore"));
        var Validcount = parseInt(
          record.get("ntotalcallcount") - parseInt(record.get("n0count"))
        );
        if(Validcount==0){return 0}
        return (varysctotalscoreore / Validcount).toFixed(2);
      },
    },
    {
      header: "Total count",
      width: 100,
      dataIndex: "ntotalcallcount",
    },

  ],
  viewConfig: {
    loadMask: true,
    enableTextSelection: true,
  },
});
