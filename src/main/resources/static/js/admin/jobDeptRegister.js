function searchJob() {
   document.searchJobFrm.jobkeyword.value = document.jobFrm.searchJobkeyword.value;
   document.searchJobFrm.submit();
}

function searchDept() {
   document.searchDeptFrm.deptkeyword.value = document.deptFrm.searchDeptkeyword.value;
   document.searchDeptFrm.submit();
}

function handleJobEnterKey(e) {
   if (e.keyCode === 13) {
      e.preventDefault();
      searchDept();
   }
}

function handleDeptEnterKey(e) {
   if (e.keyCode === 13) {
      e.preventDefault();
      searchDept();
   }
}