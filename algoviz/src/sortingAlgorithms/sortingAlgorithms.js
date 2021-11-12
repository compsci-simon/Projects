const ANIMATION_TIME = 10000.0

export function insertionSortAnimation(list) {
  var animations = []
  var operations = 0
  
  for (let i = 0; i < list.length; i++) {
    for (let j = i; j > 0; j--) {
      animations.push({"compare": [j, j - 1]})
      operations++;
      if (list[j] < list[ j - 1]) {
        let temp = list[j - 1];
        list[j - 1] = list[j];
        list[j] = temp;
        animations.push({"swap": {0:[j, list[j]], 1:[j - 1, list[j - 1]]}});
        operations++;
      } else {
        break;
      }
    }
  }
  return {
    "duration":ANIMATION_TIME/operations, 
    "animations":animations
  }
}

export function selectionSort(list) {
  var animations = []
  var operations = 0

  for (let i = 0; i < list.length; i++) {
    let min = i;
    for (let j = i + 1; j < list.length; j++) {
      animations.push({"compare": [j, min]})
      if (list[j] < list[min]) {
        min = j;
      }
      operations++;
    }
    let temp = list[i];
    list[i] = list[min];
    list[min] = temp;
    operations++;
    animations.push({"swap": {0:[i, list[i]], 1:[min, list[min]]}});
  }


  return {
    "duration":ANIMATION_TIME/operations, 
    "animations":animations
  }
}