package ideafocus.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

@State(
        name = "FocusConfig",
        storages = {
               @Storage(file = "FocusConfig.xml")
        }
)
public class FocusConfig implements PersistentStateComponent<FocusConfig.State> {
   public static class State {
       public int completedSessionCount;
       public long currentDay;
       public int focusLength = 25;

       public boolean equals(Object o) {
            if (o == this) return true;
            if (o == null || o.getClass() != this.getClass()) return false;

            State oState = (State)o;
            return oState.completedSessionCount == this.completedSessionCount &&
                    oState.currentDay == this.currentDay &&
                    oState.focusLength == this.focusLength;
       }
   }

   State myState = new State();

   public State getState() {
       return myState;
   }

   public void loadState(State s) {
       myState = s;
   }

}
