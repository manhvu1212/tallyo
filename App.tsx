import { NavigationContainer, DefaultTheme } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { StatusBar } from 'expo-status-bar';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AddRoundScreen } from './src/screens/AddRoundScreen';
import { NewSessionScreen } from './src/screens/NewSessionScreen';
import { SessionDetailScreen } from './src/screens/SessionDetailScreen';
import { SessionsListScreen } from './src/screens/SessionsListScreen';
import { StatsScreen } from './src/screens/StatsScreen';
import { StoreProvider } from './src/store';
import { colors } from './src/theme';
import type { RootStackParamList } from './src/navigation';

const Stack = createNativeStackNavigator<RootStackParamList>();

const navTheme = {
  ...DefaultTheme,
  dark: true,
  colors: {
    ...DefaultTheme.colors,
    background: colors.bg,
    card: colors.bg,
    text: colors.text,
    border: colors.border,
    primary: colors.primary,
    notification: colors.primary,
  },
};

export default function App() {
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <StoreProvider>
          <StatusBar style="light" />
          <NavigationContainer theme={navTheme}>
            <Stack.Navigator
              screenOptions={{
                headerStyle: { backgroundColor: colors.bg },
                headerTitleStyle: { color: colors.text },
                headerTintColor: colors.primary,
                contentStyle: { backgroundColor: colors.bg },
              }}
            >
              <Stack.Screen
                name="SessionsList"
                component={SessionsListScreen}
                options={{ headerShown: false }}
              />
              <Stack.Screen
                name="NewSession"
                component={NewSessionScreen}
                options={{ title: 'Buổi chơi mới' }}
              />
              <Stack.Screen
                name="SessionDetail"
                component={SessionDetailScreen}
              />
              <Stack.Screen name="AddRound" component={AddRoundScreen} />
              <Stack.Screen
                name="Stats"
                component={StatsScreen}
                options={{ title: 'Thống kê' }}
              />
            </Stack.Navigator>
          </NavigationContainer>
        </StoreProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
