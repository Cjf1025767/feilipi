Ext.define("Tab.view.report.reportIvrSummary", {
  extend: "Ext.grid.Panel",
  xtype: "reportIvrSummary",
  //通快 呼入队列汇总 按部门分组的
  requires: [
    "Tab.view.report.reportIvrSummaryController",
    "Tab.view.report.reportIvrWebSummaryModel",
    "Tab.view.report.reportIvrSummaryModel",
    "Tab.view.report.reportIvrSummaryStore",
    "Ext.form.field.Spinner",
    "Ext.form.RadioGroup",
    "Ext.form.field.Radio",
  ],

  controller: "report-reportIvrSummary",
  viewModel: {
    type: "report-reportIvrSummary",
  },

  store: Ext.create("Tab.view.report.reportIvrSummaryStore"),
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
            text: "Call Summary",
          },
          {
            xtype: "button",
            width: 100,
            margin: "0 0 10 8",
            text: "Query",
            handler: function () {
              var grid = this.up("gridpanel");
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
                starttime: grid.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
                endtime:grid.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
                type: grid.down("radiogroup").getChecked()[0].inputValue,
                department: grid.down('combobox[name="department"]').getValue(),
                queues: grid.down('tagfield[fieldLabel="MoreQueues"]').getValue().toString(),
                firstdigits:  grid.down('tagfield[fieldLabel="FirstDigits"]').getValue().toString(),
                seconddigits: grid.down('tagfield[fieldLabel="SecondDigits"]').getValue().toString(),
                  
              };
              grid.store.load();
            },
          },
          {
				xtype: 'button',
				width: 100,
				margin: '0 0 10 8',
				text: 'Export',
				handler: function () {
					 var grid = this.up("gridpanel");
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
		              var form = Ext.create('Ext.form.Panel');
                      var path = window.location.pathname;
                      var pos = window.location.pathname.lastIndexOf('/');
                      if(pos>0){
                          path = window.location.pathname.substring(0,pos) + '/';
                      }
                      form.submit({
                        target: '_blank',
                        standardSubmit: true,
                        url: '/tab/call/ReportQueuesCallSummaryByQueues',
                        method: 'POST',
                        hidden: true,
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        params: {
                           script: path +'ReportIvrSummaryExport.js',
                            starttime: grid.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
                            endtime:grid.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
                            type: grid.down("radiogroup").getChecked()[0].inputValue,
                            department: grid.down('combobox[name="department"]').getValue(),
                            queues: grid.down('tagfield[fieldLabel="MoreQueues"]').getValue().toString(),
                            firstdigits:  grid.down('tagfield[fieldLabel="FirstDigits"]').getValue().toString(),
                            seconddigits: grid.down('tagfield[fieldLabel="SecondDigits"]').getValue().toString(),
                        }
                    });
                    Ext.defer(function () {
                        form.close(); //延迟关闭表单(不会影响浏览器下载)
                    }, 100);
					}
			}
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
            width: 240,
            margin: "0 0 5 9",
            name: "department",
            forceSelection: true,
            fieldLabel: "Department",
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
            xtype: "container",
            layout: "vbox",
            items: [
              {
                xtype: "radiogroup",
                width: 300,
                layout: {
                  align: "middle",
                  type: "hbox",
                },
                items: [
                  {
                    xtype: "radiofield",
                    name: "detailSummaryType",
                    inputValue: "3",
                    boxLabel: "Month",
                    margin: "0 0 0 10",
                  },
                  {
                    xtype: "radiofield",
                    name: "detailSummaryType",
                    inputValue: "2",
                    boxLabel: "Week",
                    margin: "0 0 0 10",
                  },
                  {
                    xtype: "radiofield",
                    name: "detailSummaryType",
                    inputValue: "1",
                    checked: true,
                    boxLabel: "Day",
                    margin: "0 0 0 10",
                  },
                  {
                    xtype: "radiofield",
                    name: "detailSummaryType",
                    inputValue: "0",
                    boxLabel: "Hour",
                    margin: "0 0 0 10",
                  },
                ],
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
      width: 80, //租赁
      align: "center",
    },
    {
      header: "Time",
      width: 85,
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
              "- " +
              record.get("nhour") +
              "Hour"
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
            return (
              record.get("nyear") + "-" + record.get("nmonth")
            );
        }
      },
    },
    {
      header: "Department",
      width: 150,
      dataIndex: "department",
      align: "center",
    },
    {
      header: "InBoundCount",
      width: 150,
      align: "center",
      dataIndex: "ninboundcount",
    },
    {
      header: "NoAnswerCount",
      width: 150,
      align: "center",
      dataIndex: "nnoanswercount",
    },
    {
      header: "AnsweredRate",
      width: 150,
      align: "center",
      dataIndex: "",
      renderer: function (
        v,
        metaData,
        record,
        rowIndex,
        colIndex,
        store,
        view
      ) {
        var answered = record.get("ninboundcount")
          ? parseInt(record.get("ninboundcount"))
          : 0;
        var noanswerd = record.get("nnoanswercount")
          ? parseInt(record.get("nnoanswercount"))
          : 0;
        var nNoAnswerLength = record.get("nnoanswerlength")
          ? parseInt(record.get("nnoanswerlength"))
          : 0;
          var nnoivranswerlength = record.get("nnoivranswerlength")
          ? parseInt(record.get("nnoivranswerlength"))
          : 0;
        var total = answered + noanswerd + nNoAnswerLength+nnoivranswerlength;

        return answered == 0
          ? "0%"
          : Math.round((answered / total) * 10000) / 100.0 + "%";
      },
    },
    {
      header: "AbandonedWhileQueue",
      width: 160,
      align: "center",
      dataIndex: "nnoanswerlength",
    },
    {
      header: "AbandonedWhileIvr",
      width: 160,
      align: "center",
      dataIndex: "nnoivranswerlength",
    },
    {
      header: "InBoundWait(average)",
      width: 180,
      align: "center",
      dataIndex: "ninboundwait",
      renderer: function (
        v,
        metaData,
        record,
        rowIndex,
        colIndex,
        store,
        view
      ) {
        return standartLength(v);
      },
    },
    {
      header: "NoAnswerWait(average)",
      width: 170,
      align: "center",
      dataIndex: "nnoanswerwait",
      renderer: function (
        v,
        metaData,
        record,
        rowIndex,
        colIndex,
        store,
        view
      ) {
        return standartLength(v);
      },
    },
    {
      header: "IVRLength(average)",
      width: 150,
      align: "center",
      dataIndex: "ninboundlength",
      renderer: function (
        v,
        metaData,
        record,
        rowIndex,
        colIndex,
        store,
        view
      ) {
        return standartLength(v);
      },
    },
  ],
  viewConfig: {
    loadMask: true,
    enableTextSelection: true,
  },
});
