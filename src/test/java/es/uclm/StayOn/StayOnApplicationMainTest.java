package es.uclm.StayOn;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
class StayOnApplicationMainTest {

	@Test
	void main_runs() {
	    assertDoesNotThrow(() ->
	        StayOnApplication.main(new String[] {})
	    );
	}
}



