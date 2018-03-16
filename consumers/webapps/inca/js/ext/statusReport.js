document.write('<h1>* Status Report</h1><table width="500" cellspacing="15"><tr><td>Edit the values below to customize the series included in the status report of graphs and error messages.</td><td>[<a href="javascript:helplink(\'http://inca.sdsc.edu/releases/latest/guide/userguide-consumer.html#PASS-FAIL-REPORT\')\;">help</a>]</td></tr></table>');

Ext.onReady(function() {
  createTreeStatus();
});

function saveTreeStatus(tree) {
  var xml = tree.toXmlString();
  Ext.Ajax.request( { 
    url: 'admin.jsp', 
    params: {xml: xml, file: 'statusReport'}, 
    method: 'POST',
    success: function(){ Ext.Msg.alert("File Saved"); } 
  });
}

function createTreeStatus() {
  var tree = new Ext.tree.TreePanel({
    el:'statusReport',
    width:500,
    autoHeight:true,
    autoScroll:true,
    expandable:true,
    enableDD:true,
    title: 'Status report graphed series',
    loader: new Ext.ux.XmlTreeLoader({ preloadChildren: true }),
    root: Ext.Ajax.request({
      url: '/inca/xml/statusReport.xml',
      success: function(response){
        var root = response.responseXML.documentElement || response.responseXML;
        var node = new Ext.tree.AsyncTreeNode({ text: root.tagName, xmlNode: root });
        tree.setRootNode(node);
        tree.expandAll();
        tree.collapseAll();
        tree.root.expand();
      }
    }),
    listeners: {
      'textchange' : function(){ saveTreeStatus(tree); }
    },
    tbar: [{
      text:'+', listeners: { 'click' : function(){ tree.expandAll(); } }
    },{ xtype:'tbseparator' },{
      text:'-', listeners: { 'click' : function(){ tree.collapseAll(); tree.root.expand(); } }
    },{ xtype:'tbseparator' },{
      text:'Add Status Graph',
      listeners: {
        'click' : function(){
          handleCreate = function (btn, text, cBoxes){
            if(btn == 'ok' && text) {
              var graph = new Ext.tree.TreeNode({text:'graph', leaf:false, expandable:true });
              graph.appendChild( new Ext.tree.TreeNode({text:'series', leaf:false, expandable:true }) );
              graph.firstChild.appendChild( new Ext.tree.TreeNode({text:text, leaf:true, expandable:false }) );
              graph.appendChild( new Ext.tree.TreeNode({text:'title', leaf:false, expandable:true }) );
              graph.item(1).appendChild( new Ext.tree.TreeNode({text:'', leaf:true, expandable:false }) );
              var root = tree.getRootNode();
              root.insertBefore(graph, root.firstChild);
              saveTreeStatus(tree);
            }
          }
          Ext.MessageBox.show({ title:'Add Status Graph', msg: 'Series to graph (enter as "nickname,resource,label"):', buttons: Ext.MessageBox.OKCANCEL, prompt:true, fn: handleCreate });
        }
      }
    },{ xtype:'tbseparator' },{
      text:'Add Metric Graph',
      listeners: {
        'click' : function(){
          handleCreate = function (btn, text, cBoxes){
            if(btn == 'ok' && text) {
              var graph = new Ext.tree.TreeNode({text:'graph', leaf:false, expandable:true });
              graph.appendChild( new Ext.tree.TreeNode({text:'chart', leaf:false, expandable:true }) );
              graph.firstChild.appendChild( new Ext.tree.TreeNode({text:'metric', leaf:true, expandable:false }) );
              graph.appendChild( new Ext.tree.TreeNode({text:'metric', leaf:false, expandable:true }) );
              graph.item(1).appendChild( new Ext.tree.TreeNode({text:'metric_name', leaf:true, expandable:false }) );
              graph.appendChild( new Ext.tree.TreeNode({text:'series', leaf:false, expandable:true }) );
              graph.item(2).appendChild( new Ext.tree.TreeNode({text:text, leaf:true, expandable:false }) );
              graph.appendChild( new Ext.tree.TreeNode({text:'titlePrefix', leaf:false, expandable:true }) );
              graph.item(3).appendChild( new Ext.tree.TreeNode({text:'Metrics (', leaf:true, expandable:false }) );
              graph.appendChild( new Ext.tree.TreeNode({text:'titleSuffix', leaf:false, expandable:true }) );
              graph.item(4).appendChild( new Ext.tree.TreeNode({text:')', leaf:true, expandable:false }) );
              var root = tree.getRootNode();
              root.insertBefore(graph, root.firstChild);
              saveTreeStatus(tree);
            }
          }
          Ext.MessageBox.show({ title:'Add Metric Graph', msg: 'Series to graph (enter as "nickname,resource,label"):', buttons: Ext.MessageBox.OKCANCEL, prompt:true, fn: handleCreate });
        }
      }
    },{ xtype:'tbseparator' },{
      text:'Add Series',
      listeners: {
        'click' : function(){
          var selected = tree.getSelectionModel().getSelectedNode();
          if (!selected || (selected.parentNode.text != "statusReport" || selected.text != "graph")){
            Ext.Msg.alert("You must first select a 'graph' folder to add a series.");
          } else {
            handleCreate = function (btn, text, cBoxes){
              if(btn == 'ok' && text) {
                var series = new Ext.tree.TreeNode({text:'series', leaf:false, expandable:true });
                series.appendChild( new Ext.tree.TreeNode({text:text, leaf:true, expandable:false }) );
                selected.insertBefore(series, selected.item(1));
                saveTree(tree);
              }
            }
            Ext.MessageBox.show({ title:'Add Series', msg: 'Series (enter as "nickname,resource,label"):', buttons: Ext.MessageBox.OKCANCEL, prompt:true, fn: handleCreate });
          }
        }
      }
    },{ xtype:'tbseparator' },{
      text:'Delete',
      listeners: {
        'click' : function(){
          var selectedItem = tree.getSelectionModel().getSelectedNode();
          if (!selectedItem) {
            Ext.Msg.alert('Warning', 'Please select an Item to delete.');
            return false;
          }
          handleDelete = function (btn){
            if(btn == 'ok') { 
              selectedItem.remove(); 
              saveTreeStatus(tree);
            }
          }
          var text = selectedItem.text;
          Ext.MessageBox.show({
            title:'Confirm your action',
            msg: "Are you sure you want to delete '"+text+"' and everything under it?",
            buttons: Ext.MessageBox.OKCANCEL,
            fn: handleDelete
          });
        }
      }
    },{ xtype:'tbseparator' },{
      text: 'Restore to Defaults',
      listeners: {
        'click' : function(){
          handleDefault = function (btn){
            if(btn == 'ok') {  
              Ext.Ajax.request({ 
                url: 'admin.jsp', 
                params: {defaults: 'statusReport'}, 
                method: 'POST', 
                success: function(){ window.location.reload(true); Ext.Msg.alert("Defaults Restored"); } 
              });
            }
          }
          Ext.MessageBox.show({
            title:'Confirm your action',
            msg: 'Are you sure you want to restore defaults?',
            buttons: Ext.MessageBox.OKCANCEL,
            fn: handleDefault
          });
        }
      }
    }]
  });
  tree.render();
  var te = new Ext.tree.TreeEditor(tree, null, {
    editDelay: 0,
    beforeNodeClick : Ext.emptyFn,
    onNodeDblClick : function(node, e){
      e.stopEvent();
      this.triggerEdit(node);
    }
  });
}
