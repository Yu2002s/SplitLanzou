(function() {
    let disk = document.querySelector('.mydisk_bar');
    let username = disk.childNodes[0].data.trim();
    let src = document.querySelector('#mainframe').getAttribute('src');
    src = src.substring(src.lastIndexOf('u=') + 2);
    local_obj.saveUser(parseInt(src), username, document.cookie);
})();
