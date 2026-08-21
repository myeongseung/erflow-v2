const inputs = document.querySelectorAll(".input");
    function addcl(){
        let parent = this.parentNode.parentNode;
        parent.classList.add("focus");
        }

        function remcl(){
        let parent = this.parentNode.parentNode;
        if(this.value == ""){
            parent.classList.remove("focus");
        }
    }
    inputs.forEach(input => {
    input.addEventListener("focus", addcl);
    input.addEventListener("blur", remcl);
});

document.querySelector("form").addEventListener("submit", function(e) {
    const username = document.querySelector(".input-div.one .input").value;
    const password = document.querySelector(".input-div.pass .input").value;

    if (!username) {
        alert("모두 입력해 주세요!!!");
        e.preventDefault();
        return;
    }

    if (!password) {
        alert("모두 입력해 주세요!!!");
        e.preventDefault();
    }
});