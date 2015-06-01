var View = require('ampersand-view');
var AccountView = require('../views/account-dropdown');

module.exports = View.extend({
    render: function() {

        this.renderCollection(this.collection, AccountView, this.el);
        return this;
    }
});
