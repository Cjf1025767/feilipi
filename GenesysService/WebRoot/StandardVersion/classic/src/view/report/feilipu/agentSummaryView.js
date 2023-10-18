Ext.define("Tab.view.report.tongkuai.agentSummaryView", {
  extend: "Ext.grid.Panel",
  xtype: "agentSummary",
  requires: [
    "Tab.view.report.feilipu.agentSummaryModel",
    "Tab.view.report.feilipu.agentSummaryStore",
    "Ext.form.field.Spinner",
  ],
  store: Ext.create("Tab.view.report.feilipu.agentSummaryStore"),
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
            text: "Agent Summary",
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
                  agent: grid.down('textfield[name="agent"]').getValue(),
                  starttime:
                    grid
                      .down('datetimetowfield[name="starttime"]')
                      .getSubmitValue() + ":00",
                  endtime:
                    grid
                      .down('datetimetowfield[name="endtime"]')
                      .getSubmitValue() + ":59",
                  type: grid.down("radiogroup").getChecked()[0].inputValue,
                  agentname: grid
                    .down('textfield[name="agentname"]')
                    .getValue(),
                    agent_group: grid
                    .down('textfield[name="agent_group"]')
                    .getValue(),
                    
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
                      url: "/tab/FLPrec/UISearchAgentSummary",
                      method: "POST",
                      hidden: true,
                      headers: {
                        "Content-Type": "application/x-www-form-urlencoded",
                      },
                      params: {
                        script: path + "agentSummaryEeport.js",
                        agent: grid.down('textfield[name="agent"]').getValue(),
                        starttime:
                          grid
                            .down('datetimetowfield[name="starttime"]')
                            .getSubmitValue() + ":00",
                        endtime:
                          grid
                            .down('datetimetowfield[name="endtime"]')
                            .getSubmitValue() + ":59",
                        type: grid.down("radiogroup").getChecked()[0]
                          .inputValue,
                        agentname: grid
                          .down('textfield[name="agentname"]')
                          .getValue(),
                          agent_group: grid
                          .down('textfield[name="agent_group"]')
                          .getValue(),
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
            xtype: "textfield",
            width: 200,
            name: "agent",
            fieldLabel: "Agent",
            labelWidth: 80,
          },
          {
            xtype: "textfield",
            name: "agentname",
            margin: "0 30 0 0",
            width: 200,
            fieldLabel: "AgentName",
            labelWidth: 80,
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
                  name: "OUTBOUND",
                  id: 2,
                },
                {
                  name: "INTERNAL",
                  id: 3,
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
            name: "agent_group",
            margin: "0 30 0 0",
            width: 200,
            fieldLabel: "AgentGroup",
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
      header: "agent",
      width: 150,
      dataIndex: "resource_name",
      align: "center",
    },
    {
        header: "agent name",
        width: 150,
        dataIndex: "user_name",
        align: "center",
      },
      {
        header: "agent group",
        width: 150,
        dataIndex: "agent_group",
        align: "center",
      },
    
    {
      header: "time",
      width: 120,
      dataIndex: "time",
      align: "center",
    },
    {
      header: "accepted",
      width: 120,
      dataIndex: "accepted",
      align: "center",
    },
    {
      header: "notaccepted",
      width: 120,
      dataIndex: "notaccepted",
      align: "center",
    },
    {
      header: "offered",
      width: 120,
      dataIndex: "offered",
      align: "center",
    },
    {
      header: "abandoned invite",
      width: 120,
      dataIndex: "abandoned_invite",
      align: "center",
    },
    {
      header: "rejected",
      width: 120,
      dataIndex: "rejected",
      align: "center",
    },
    {
      header: "invite",
      width: 120,
      dataIndex: "invite",
      align: "center",
    },
    {
      header: "invite time",
      width: 120,
      dataIndex: "invite_time",
      align: "center",
      renderer: function (value, cell, records) {
        return standartLength(value);
      },
    },
    {
      header: "engage time",
      width: 120,
      dataIndex: "engage_time",
      align: "center",
      renderer: function (value, cell, records) {
        return standartLength(value);
      },
    },
    {
      header: "engage",
      width: 120,
      dataIndex: "engage",
      align: "center",
    },
    {
      header: "hold time",
      width: 120,
      dataIndex: "hold_time",
      align: "center",
      renderer: function (value, cell, records) {
        return standartLength(value);
      },
    },
    {
      header: "wrap ime",
      width: 120,
      dataIndex: "wrap_time",
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
      header: "consult received accepted",
      width: 120,
      dataIndex: "consult_received_accepted",
      align: "center",
    },

    {
      header: "consult received engage time",
      width: 120,
      dataIndex: "consult_received_engage_time",
      align: "center",
      renderer: function (value, cell, records) {
        return standartLength(value);
      },
    },
    {
      header: "transfer init agent",
      width: 120,
      dataIndex: "transfer_init_agent",
      align: "center",
    },
    {
      header: "xfer received accepted",
      width: 120,
      dataIndex: "xfer_received_accepted",
      align: "center",
    },
    {
      header: "media name",
      width: 120,
      dataIndex: "media_name",
      align: "center",
    },

    {
      header: "interaction type code",
      width: 120,
      dataIndex: "interaction_type_code",
      align: "center",
    },
    {
      header: "interaction subtype code",
      width: 120,
      dataIndex: "interaction_subtype_code",
      align: "center",
    },
  ],
  viewConfig: {
    loadMask: true,
    enableTextSelection: true,
  },
});
