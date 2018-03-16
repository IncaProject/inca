document.write('<h1>* Run Nows</h1><table width="500" cellspacing="15"><tr><td>If the "Allow?" option is checked a "Run Now" button will appear under the "Command used to execute the reporter" heading of the reporter details status pages. Clicking on the "Run Now" button triggers reporter execution on the local machine and requires a password by default.</td><td>[<a href="javascript:helplink(\'http://inca.sdsc.edu/releases/latest/guide/userguide-consumer.html#CONSUMER-RUN-NOW\')\;">help</a>]</td></tr></table>');

Ext.onReady(function(){
  var checkColumn = new Ext.grid.CheckColumn({ header: "Allow?", dataIndex: "allowRunNow", width: 50 });
  var store = new Ext.data.Store({
    url: '/inca/xml/runNow.xml',
    reader: new Ext.data.XmlReader({ record: '' }, Ext.data.Record.create([{ name: 'allowRunNow', type: 'bool', id: 'allowRunNow'}])),
    autoSave: false
  });
  var grid = new Ext.grid.EditorGridPanel({
    store: store,
    cm: new Ext.grid.ColumnModel([ checkColumn ]),
    renderTo: 'runNow',
    width:500,
    height:125,
    title:'Run nows from web page',
    plugins:checkColumn,
    clicksToEdit:1,
    tbar: [{
      text: 'Restore to Defaults',
      listeners: {
        'click' : function(){
          handleDefault = function (btn){
            if(btn == 'ok') {
              Ext.Ajax.request({
                url: 'admin.jsp',
                params: {defaults: 'runNow'},
                method: 'POST',
                success: function(){ store.load(); }
              });
            }
          }
          Ext.MessageBox.show({
            title:'Confirm your action',
            msg: 'Are you sure you want to restore defaults?',
            buttons: Ext.MessageBox.OKCANCEL,
            fn: handleDefault
          });
        }, scope:this }
    }]
  });
  // trigger the data store load
  store.load();
});

Ext.grid.CheckColumn = function(config){
  Ext.apply(this, config);
  if(!this.id){ this.id = Ext.id(); }
  this.renderer = this.renderer.createDelegate(this);
};

Ext.grid.CheckColumn.prototype ={
  init : function(grid){
    this.grid = grid;
    this.grid.on('render', function(){
      var view = this.grid.getView();
      view.mainBody.on('mousedown', this.onMouseDown, this);
    }, this);
  },
  onMouseDown : function(e, t){
    if(t.className && t.className.indexOf('x-grid3-cc-'+this.id) != -1){
      e.stopEvent();
      var index = this.grid.getView().findRowIndex(t);
      var record = this.grid.store.getAt(index);
      record.set(this.dataIndex, !record.data[this.dataIndex]);
      var xml = "<incaConsumerConfig>\n  " + record.asXml() + "\n</incaConsumerConfig>\n";
      Ext.Ajax.request( { url: 'admin.jsp', params: {xml: xml, file: 'runNow'}, method: 'POST' });
    }
  },
  renderer : function(v, p, record){
    p.css += ' x-grid3-check-col-td';
    return '<div class="x-grid3-check-col'+(v?'-on':'')+' x-grid3-cc-'+this.id+'">&#160;</div>';
  }
};
