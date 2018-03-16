Ext.override(Ext.data.Record, {
  asXml : function(rowIndex) {
    var r = this.store.reader.meta;
    var elName = r.record;
    var result = "";
    this.fields.each(function(f) {
      result += "<" + f.name + ">" + this.get(f.name) + "</" + f.name + ">";
    }, this);
    return result;
  }
});

Ext.ux.XmlTreeLoader = Ext.extend(Ext.tree.TreeLoader, {
/**
* Load an {@link Ext.tree.TreeNode} xmlNode referred to by the passed TreeNode.
* This is called automatically when a node is expanded, but may be used to reload
* a node (or append new children if the {@link #clearOnLoad} option is false.)
* @param {Ext.tree.TreeNode} node The existing node for which to load child nodes.
* It must have a reference to its corresponding node in the XML document in a property
* called xmlNode.
* @param {Function} callback
*/
  load : function(node, callback){
    if(this.clearOnLoad){
      while(node.firstChild){ node.removeChild(node.firstChild); }
    }
    if(this.doPreload(node)){ // preloaded json children
      if(typeof callback == "function"){ callback(); }
    }else {
      this.loadXml(node, callback);
    }
  },
  doPreload : function(node){
    if(node.attributes.children){
      if(node.childNodes.length < 1){ // preloaded?
        var cs = node.attributes.children;
        node.beginUpdate();
        for(var i = 0, len = cs.length; i < len; i++){
          var cn = node.appendChild(this.createNode(cs[i]));
          if(this.preloadChildren){
            this.doPreload(cn);
          }
        }
        node.endUpdate();
      }
      return true;
    }else {
      return false;
    }
  },
  loadXml : function(node, callback){
    var xNode = node.attributes.xmlNode;
    // If the TreeNode's xmlNode is an Element, or a Document then we can load from it.
    if (xNode && ((xNode.nodeType == 1) || (xNode.nodeType == 9))) {
      // Load attributes as child nodes
      var childNodes = xNode.attributes, l = xNode.attributes.length;
      for (var i = 0; i < l; i++) {
        var c = xNode.attributes[i];
        node.appendChild(this.createNode({
          iconCls: 'attribute-name',
          text: c.name,
          children: [{
            iconCls: 'attribute-value',
            text: c.value,
            leaf: true
          }]
        }));
      }
      // Load child elements as child nodes
      childNodes = xNode.childNodes, l = xNode.childNodes.length;
      for (var i = 0; i < l; i++) {
        var c = xNode.childNodes[i];
        if (c.nodeType == 1) {
          node.appendChild(this.createNode({
            text: c.tagName,
            xmlNode: c,
            leaf: ((c.childNodes.length + c.attributes.length) == 0)
          }));
        } else if ((c.nodeType == 3) && (c.data.trim().length != 0)) {
          node.appendChild(this.createNode({
            text: c.data,
            leaf: true
          }));
        }
      }
    }
    callback(this, node);
  },
  // Override this function for custom TreeNode node implementation
  createNode : function(attr){
    if(this.baseAttrs){ Ext.applyIf(attr, this.baseAttrs); }
    if(this.applyLoader !== false){ attr.loader = this; }
    if(typeof attr.uiProvider == 'string'){
      attr.uiProvider = this.uiProviders[attr.uiProvider] || eval(attr.uiProvider);
    }
    return(attr.leaf ? new Ext.tree.TreeNode(attr) : new Ext.tree.AsyncTreeNode(attr));
  }
});
/* tostring.js */

/**
 * Returns a string of XML that represents the tree
 * @return {String}
 */
Ext.tree.TreePanel.prototype.toXmlString = function(){
  return this.getRootNode().toXmlString();
};

/**
 * Returns a string of XML that represents the node
 * @return {String}
 */
Ext.tree.TreeNode.prototype.toXmlString = function(){
  var leaf = this.attributes['leaf'];
  var text = this.attributes['text'];
  result = "";
  // Add child nodes if any
  var children = this.childNodes;
  var clen = children.length;
  if(clen == 0){ 
    result += text;
  }else{
    result += "\n<" + text + ">";
    for(var i = 0; i < clen; i++){
      result += children[i].toXmlString();
    }
    result += "</" + text + ">";
  }
  return result;
};

/**
 * Pop-up window for help links
 */
function helplink(url) {  
  var newwindow = window.open(url,'name','height=300,width=600,left=500,top=100,scrollbars=1,resizable=1,status=1,location=1,toolbar=1');
  if (window.focus) {newwindow.focus()}
}
