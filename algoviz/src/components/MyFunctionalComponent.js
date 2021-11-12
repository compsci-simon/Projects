function MyFunctionalComponent (props) {

  const list = []

  for (let i = 0; i < 300; i++) {
    const num = Math.max(5, Math.floor(Math.random() * 1000))
    list.push(num)
  }

  return (
    <div>
      {list.map((height, index) => (
        <div>{height}<br/></div>
      ))}
    </div>
  );
}

export default MyFunctionalComponent;