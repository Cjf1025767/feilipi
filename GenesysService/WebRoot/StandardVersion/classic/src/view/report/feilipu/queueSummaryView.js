Ext.define("Tab.view.report.tongkuai.queueSummaryView", {
  extend: "Ext.grid.Panel",
  xtype: "queueSummary",
  requires: [
    "Tab.view.report.feilipu.queueSummaryModel",
    "Tab.view.report.feilipu.queueSummaryStore",
    "Ext.form.field.Spinner",
  ],
  store: Ext.create("Tab.view.report.feilipu.queueSummaryStore"),
  dockedItems: [
    {
      xtype: "pagingtoolbar",
      dock: "bottom",
      pageSize: 10,
      displayInfo: true,
      displayMsg: "Display data from {0} to {1}, a total of {2}",
      emptyMsg: "No record",
      listeners: {
        beforerender: function (bgbar) {
          bgbar.add("-");
          bgbar.add(
            new Ext.form.NumberField({
              minWidth: 90,
              maxWidth: 90,
              minValue: 1,
              maxValue: 1000,
              step: 10,
              value: bgbar.up().store.pageSize,
              listeners: {
                change: function (field, newValue) {
                  bgbar.pageSize = newValue;
                  bgbar.up().store.pageSize = newValue;
                },
              },
            })
          );
          bgbar.add("Records/Page");
          bgbar.setStore(bgbar.up().store);
        },
      },
    },
  ],
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
            text: "Queue Summary",
          },
          {
            xtype: "button",
            width: 100,
            margin: "0 0 10 8",
            text: "Query",
            listeners: {
              click: function () {
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
                  grid.down('datetimetowfield[name="endtime"]').getValue() ===
                    ""
                ) {
                  grid.down('datetimetowfield[name="endtime"]').focus();
                }
                grid.store.proxy.extraParams = {
                  starttime:
                    grid
                      .down('datetimetowfield[name="starttime"]')
                      .getSubmitValue() + ":00",
                  queuename: grid
                    .down('textfield[name="queuename"]')
                    .getValue(),
                  endtime:
                    grid
                      .down('datetimetowfield[name="endtime"]')
                      .getSubmitValue() + ":59",
                  type: grid.down("radiogroup").getChecked()[0].inputValue,
                  inbound: grid.down('combobox[fieldLabel="State"]').getValue(),
                };
                grid.store.loadPage(1);
              },
            },
            menu: [
              {
                text: "Export",
                listeners: {
                  click: function (button) {
                    var grid = this.up("gridpanel");
                    if (
                      grid
                        .down('datetimetowfield[name="starttime"]')
                        .getValue() === null ||
                      grid
                        .down('datetimetowfield[name="starttime"]')
                        .getValue() === ""
                    ) {
                      grid.down('datetimetowfield[name="starttime"]').focus();
                    }
                    if (
                      grid
                        .down('datetimetowfield[name="endtime"]')
                        .getValue() === null ||
                      grid
                        .down('datetimetowfield[name="endtime"]')
                        .getValue() === ""
                    ) {
                      grid.down('datetimetowfield[name="endtime"]').focus();
                    }
                    var form = Ext.create("Ext.form.Panel");
                    var path = window.location.pathname;
                    var pos = window.location.pathname.lastIndexOf("/");
                    if (pos > 0) {
                      path = window.location.pathname.substring(0, pos) + "/";
                    }
                    form.submit({
                      target: "_blank",
                      standardSubmit: true,
                      url: "/tab/FLPrec/UISearchQueuesSummary",
                      method: "POST",
                      hidden: true,
                      headers: {
                        "Content-Type": "application/x-www-form-urlencoded",
                      },
                      params: {
                        script: path + "queueSummaryExport.js",
                        starttime:
                          grid
                            .down('datetimetowfield[name="starttime"]')
                            .getSubmitValue() + ":00",
                        queuename: grid
                          .down('textfield[name="queuename"]')
                          .getValue(),
                        endtime:
                          grid
                            .down('datetimetowfield[name="endtime"]')
                            .getSubmitValue() + ":59",
                        type: grid.down("radiogroup").getChecked()[0]
                          .inputValue,
                        inbound: grid
                          .down('combobox[fieldLabel="State"]')
                          .getValue(),
                      },
                    });
                    Ext.defer(function () {
                      form.close(); //延迟关闭表单(不会影响浏览器下载)
                    }, 100);
                  },
                },
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
            xtype: "combobox",
            width: 170,
            fieldLabel: "State",
            labelWidth: 60,
            queryMode: "local",
            value: 0,
            store: Ext.create("Ext.data.JsonStore", {
              model: "Tab.view.record.typeModel",
              data: [
                {
                  name: "ALL",
                  id: 0,
                },
                {
                  name: "INBOUND",
                  id: 1,
                },
                {
                  name: "INTERNAL",
                  id: 2,
                },
              ],
            }),
            editable: false,
            triggerAction: "all",
            displayField: "name",
            valueField: "id",
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
                boxLabel: "Year",
                margin: "0 0 0 10",
              },
              {
                xtype: "radiofield",
                name: "investigateSummaryType",
                inputValue: "2",
                boxLabel: "Month",
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
            xtype: "textfield",
            width: 190,
            name: "queuename",
            fieldLabel: "QueueName",
            labelWidth: 80,
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
      header: "queues name",
      width: 150,
      dataIndex: "resource_name",
      align: "center",
    },
    {
      header: "Time",
      width: 150,
      dataIndex: "time",
      align: "center",
    },
    {
      header: "media_name",
      width: 120,
      dataIndex: "media_name",
      align: "center",
    },
    {
      header: "interaction_type_code",
      width: 120,
      dataIndex: "interaction_type_code",
      align: "center",
    },
    {
      header: "interaction_subtype_code",
      width: 120,
      dataIndex: "interaction_subtype_code",
      align: "center",
    },
    {
      header: "entered",
      width: 120,
      dataIndex: "entered",
      align: "center",
    },
    {
      header: "abandoned_short",
      width: 120,
      dataIndex: "abandoned_short",
      align: "center",
    },
    {
      header: "abandoned_standard",
      width: 120,
      dataIndex: "abandoned_standard",
      align: "center",
    },
    {
      header: "abandoned_invite",
      width: 120,
      dataIndex: "abandoned_invite",
      align: "center",
    },
    {
      header: "distributed_",
      width: 120,
      dataIndex: "distributed_",
      align: "center",
    },
    {
      header: "distributed_time",
      width: 120,
      dataIndex: "distributed_time",
      align: "center",
      renderer: function (value, cell, records) {
        return standartLength(value);
      },
    },

    {
      header: "redirected",
      width: 120,
      dataIndex: "redirected",
      align: "center",
    },
    {
      header: "routed_other",
      width: 120,
      dataIndex: "routed_other",
      align: "center",
    },
    {
      header: "accepted",
      width: 120,
      dataIndex: "accepted",
      align: "center",
    },
    {
      header: "accepted_thr",
      width: 120,
      dataIndex: "accepted_thr",
      align: "center",
    },
    {
      header: "accepted_agent",
      width: 120,
      dataIndex: "accepted_agent",
      align: "center",
    },
    {
      header: "accepted_agent_time",
      width: 120,
      dataIndex: "accepted_agent_time",
      align: "center",
      renderer: function (value, cell, records) {
        return standartLength(value);
      },
    },
    {
      header: "accepted_agent_thr",
      width: 120,
      dataIndex: "accepted_agent_thr",
      align: "center",
    },

    {
      header: "transfer_init_agent",
      width: 120,
      dataIndex: "transfer_init_agent",
      align: "center",
    },
    {
      header: "invite",
      width: 120,
      dataIndex: "invite",
      align: "center",
    },
    {
      header: "invite_time",
      width: 120,
      dataIndex: "invite_time",
      align: "center",
      renderer: function (value, cell, records) {
        return standartLength(value);
      },
    },
    {
      header: "engage_time",
      width: 120,
      dataIndex: "engage_time",
      align: "center",
      renderer: function (value, cell, records) {
        return standartLength(value);
      },
    },
    {
      header: "wrap",
      width: 120,
      dataIndex: "wrap",
      align: "center",
    },
    {
      header: "wrap_time",
      width: 120,
      dataIndex: "wrap_time",
      align: "center",
      renderer: function (value, cell, records) {
        return standartLength(value);
      },
    },
    {
      header: "hold",
      width: 120,
      dataIndex: "hold",
      align: "center",
    },
    {
      header: "hold_time",
      width: 120,
      dataIndex: "hold_time",
      align: "center",
      renderer: function (value, cell, records) {
        return standartLength(value);
      },
    },
    {
      header: "accepted_time",
      width: 120,
      dataIndex: "accepted_time",
      align: "center",
      renderer: function (value, cell, records) {
        return standartLength(value);
      },
    },
    {
      header: "accepted_time_max",
      align: "center",
      width: 200,
      dataIndex: "accepted_time_max",
      renderer: function (value, cell, records) {
        return standartLength(value);
      },
    },
  ],
  viewConfig: {
    loadMask: true,
    enableTextSelection: true,
  },
});
