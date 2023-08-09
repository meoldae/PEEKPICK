import { createSlice } from "@reduxjs/toolkit";

const locationSlice = createSlice({
  name: "location",
  initialState: {
    //임의 ID 값
    memberId: null,
    userPos: {
      point: {
        x: 0,
        y: 0,
      },
      distance: 0,
    },
  },
  reducers: {
    updateLoc: (state, action) => {
      const userInfo = action.payload;
      state.userPos.point.x = userInfo.point.x;
      state.userPos.point.y = userInfo.point.y;
      state.userPos.distance = userInfo.distance;
    },
  },
});

export const locationActions = locationSlice.actions;
export default locationSlice.reducer;
