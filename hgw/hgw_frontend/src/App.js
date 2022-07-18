import logo from './logo.svg';
import './App.css';

import React, {useEffect} from 'react'

import SignIn from './Component/SignIn.js'
import SignUp from './Component/SignUp.js'

import Dashboard from './Component/Dashboard.js'

import axios from 'axios'

function App() {

  let [hgwState, setHgwState] = React.useState(undefined)
  console.log(hgwState)
  if(hgwState == undefined) return(
    <div>
      <SignIn callback = {setHgwState.bind(this)} ></SignIn>
    </div>
  )
  return (
    <div>
      <Dashboard></Dashboard>
    </div>
  );
}

export default App;
