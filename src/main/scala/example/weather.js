    import axios from "axios";
  

  async function getCites() {
    const {status, data } = await axios({
      url: `http://dataservice.accuweather.com/locations/v1/topcities/150?apikey=iUxLgSJkOao2GLy68sqFnV9G62sxMpEh`,
      method: "GET"
    });
      
    // document.getElementById("display").value = data.result;
    // answer = true;
  }
 