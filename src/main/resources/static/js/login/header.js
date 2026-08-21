

function cancel(){ /* 헤더 검색창 x표시 눌었을 때*/
    document.querySelector(".main-search-border input").value = "";
}

function search(){ /* 헤더 검색창 돋보기 표시 눌렀을 때 */ 
    let text = document.querySelector(".main-search-border input").value;
    alert(text);
}

window.onload = function(){

let input = document.querySelector(".main-search-text"); /*헤더부분 검색창*/ 

input.addEventListener("keydown", function (event) { /* 검색창에서 키가 눌렀졌을 때*/
    if (event.keyCode === 13) { /* 누른 키가 enter인경우 */ 
      event.preventDefault();  /* 기본 이벤트 안 일어나게 */
      alert(input.value);
    }
  });
}
