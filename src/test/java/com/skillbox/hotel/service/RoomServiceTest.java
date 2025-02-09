package com.skillbox.hotel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skillbox.hotel.model.Room;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

class RoomServiceTest {

    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService();
    }

    @Test
    void addRoom_ShouldAddRoomToList() {
        // Arrange
        Room room = new Room(1L, "Standard", 100.0, true);

        // Act
        roomService.addRoom(room);

        // Assert
        assertThat(roomService.getAllRooms())
                .hasSize(1)
                .containsExactly(room);
    }

    @Test
    void getAvailableRooms_ShouldFilterCorrectly() {
        // Arrange
        roomService.addRoom(new Room(1L, "Standard", 100.0, true));
        roomService.addRoom(new Room(2L, "Deluxe", 200.0, false));
        roomService.addRoom(new Room(3L, "Suite", 300.0, true));

        Predicate<Room> availableAndPriceFilter = room ->
                room.isAvailable() && room.getPrice() > 150.0;

        // Act
        List<Room> result = roomService.getAvailableRooms(availableAndPriceFilter);

        // Assert
        assertThat(result)
                .hasSize(1)
                .extracting(Room::getRoomId)
                .containsExactly(3L);
    }

    @Test
    void findRoomById_ShouldReturnCorrectRoom() {
        // Arrange
        Room expected = new Room(1L, "Standard", 100.0, true);
        roomService.addRoom(expected);
        roomService.addRoom(new Room(2L, "Deluxe", 200.0, false));

        // Act
        Optional<Room> result = roomService.findRoomById(1L);

        // Assert
        assertThat(result)
                .isPresent()
                .containsSame(expected);
    }

    @Test
    void findRoomById_ShouldReturnEmptyForNonExistingId() {
        // Act
        Optional<Room> result = roomService.findRoomById(999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void updateRoomAvailability_ShouldChangeRoomStatus() {
        // Arrange
        Room room = new Room(1L, "Standard", 100.0, true);
        roomService.addRoom(room);

        // Act
        roomService.updateRoomAvailability(1L, false);

        // Assert
        assertThat(room.isAvailable()).isFalse();
    }

    @Test
    void updateRoomAvailability_ShouldDoNothingForWrongId() {
        // Arrange
        Room room = new Room(1L, "Standard", 100.0, true);
        roomService.addRoom(room);

        // Act
        roomService.updateRoomAvailability(999L, false);

        // Assert
        assertThat(room.isAvailable()).isTrue();
    }

    @Test
    void getAllRooms_ShouldReturnUnmodifiableCollection() {
        // Arrange
        Room room = new Room(1L, "Standard", 100.0, true);
        roomService.addRoom(room);

        // Act
        List<Room> result = roomService.getAllRooms();

        // Assert
        // Проверяем, что попытка модификации вызывает исключение
        assertThatThrownBy(() -> result.add(new Room(2L, "Deluxe", 200.0, false)))
                .isInstanceOf(UnsupportedOperationException.class);

        // Дополнительная проверка, что исходная коллекция не изменилась
        Assertions.assertThat(roomService.getAllRooms())
                .hasSize(1)
                .extracting(Room::getRoomId)
                .containsExactly(1L);
    }

    @Test
    void shouldHandleNullValuesSafely() {
        // Act & Assert
        Assertions.assertThatCode(() -> {
            roomService.addRoom(null);
            roomService.findRoomById(null);
            roomService.updateRoomAvailability(null, true);
        }).doesNotThrowAnyException();

        assertThat(roomService.getAvailableRooms(null)).isEmpty();
    }

    @Test
    void shouldHandleDuplicateRoomIds() {
        // Arrange
        Room room1 = new Room(1L, "Standard", 100.0, true);
        Room room2 = new Room(1L, "Deluxe", 200.0, false);

        // Act
        roomService.addRoom(room1);
        roomService.addRoom(room2);

        // Assert
        assertThat(roomService.getAllRooms())
                .hasSize(2)
                .extracting(Room::getRoomId)
                .containsExactly(1L, 1L);
    }

    @Test
    void shouldHandleEmptyRoomList() {
        // Act & Assert
        assertThat(roomService.getAllRooms()).isEmpty();
        assertThat(roomService.getAvailableRooms(r -> true)).isEmpty();
        assertThat(roomService.findRoomById(1L)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideRoomsForAddTest")
    void addRoom_ShouldCorrectlyAddOrIgnoreNull(Room room, int expectedChange) {
        roomService.addRoom(room);
        assertThat(roomService.getAllRooms()).hasSize(expectedChange);
    }

    private static Stream<Arguments> provideRoomsForAddTest() {
        return Stream.of(
                Arguments.of(new Room(4L, "Deluxe", 4, true), 1),
                Arguments.of(null, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("provideFiltersForAvailabilityTest")
    void getAvailableRooms_ShouldFilterCorrectly(Predicate<Room> filter, int expectedCount) {
        roomService.addRoom(new Room(1L, "Standard", 1, true));
        roomService.addRoom(new Room(2L, "Suite", 2, false));
        roomService.addRoom(new Room(3L, "Single", 3, true));
        assertThat(roomService.getAvailableRooms(filter)).hasSize(expectedCount);
    }

    private static Stream<Arguments> provideFiltersForAvailabilityTest() {
        final Predicate<Room> predicate1 = room -> "Suite".equals(room.getType());
        return Stream.of(
                Arguments.of((Predicate<Room>) Room::isAvailable, 2),
                Arguments.of((Predicate<Room>) room -> "Suite".equals(room.getType()), 1),
                Arguments.of(null, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("provideRoomIdsForSearchTest")
    void findRoomById_ShouldReturnCorrectResult(Long roomId, boolean expectedFound) {
        roomService.addRoom(new Room(1L, "Standard", 1, true));
        roomService.addRoom(new Room(2L, "Suite", 2, false));
        roomService.addRoom(new Room(3L, "Single", 3, true));
        Optional<Room> room = roomService.findRoomById(roomId);
        if (expectedFound) {
            assertThat(room).isPresent();
        } else {
            assertThat(room).isNotPresent();
        }
    }

    private static Stream<Arguments> provideRoomIdsForSearchTest() {
        return Stream.of(
                Arguments.of(1L, true),
                Arguments.of(4L, false),
                Arguments.of(null, false)
        );
    }

    // Тестирование обновления доступности
    @ParameterizedTest
    @MethodSource("provideAvailabilityUpdatesTest")
    void updateRoomAvailability_ShouldUpdateIfExists(Long roomId, boolean newAvailability, boolean shouldUpdate) {
        roomService.addRoom(new Room(1L, "Standard", 1, true));
        roomService.addRoom(new Room(2L, "Suite", 2, false));
        roomService.addRoom(new Room(3L, "Single", 3, true));

        roomService.updateRoomAvailability(roomId, newAvailability);
        Optional<Room> room = roomService.findRoomById(roomId);
        if (shouldUpdate) {
            assertThat(room).isPresent().hasValueSatisfying(r -> assertThat(r.isAvailable()).isEqualTo(newAvailability));
        } else {
            assertThat(room).isNotPresent();
        }
    }

    private static Stream<Arguments> provideAvailabilityUpdatesTest() {
        return Stream.of(
                Arguments.of(1L, false, true),
                Arguments.of(2L, true, true),
                Arguments.of(4L, true, false)
        );
    }

    // Тестирование получения неизменяемого списка
    @Test
    void getAllRooms_ShouldReturnImmutableList() {
        List<Room> rooms = roomService.getAllRooms();
        assertThatThrownBy(() -> rooms.add(new Room(5L, "Test", 2, false)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
